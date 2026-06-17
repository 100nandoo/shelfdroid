package dev.halim.shelfdroid.core.data.screen.edititem

import android.content.ContentResolver
import android.net.Uri
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CoverFromUrlRequest
import dev.halim.core.network.request.MatchLibraryItemRequest
import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.MatchItemResult
import dev.halim.core.network.response.SearchProviders
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.helper.getFilename
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditItemRepository
@Inject
constructor(
  private val api: ApiService,
  private val helper: Helper,
  private val libraryItemRepo: LibraryItemRepo,
) {

  suspend fun load(itemId: String): EditItemUiState {
    val item =
      api.item(itemId).getOrElse {
        return EditItemUiState(
          state = GenericState.Failure(it.message),
          itemId = itemId,
          coverUrl = helper.generateItemCoverUrl(itemId),
          webBaseUrl = "https://${DataStoreManager.BASE_URL}",
        )
      }

    val mediaKind = EditItemMapper.mapMedia(item).mediaKind
    val providersResponse = api.searchProviders().getOrNull()?.providers
    val providers = matchProvidersFor(providersResponse, mediaKind)
    val coverProviders = coverProvidersFor(providersResponse, mediaKind)
    // TODO use prefs once edit libraries implemented
    val defaultProvider = "audible"
    val defaultCoverProvider = coverProviders.firstOrNull()?.value ?: "best"

    val seriesSuggestions =
      api.librarySeries(item.libraryId).getOrNull()?.results?.map { it.name }.orEmpty()

    return mapToUiState(
      item,
      providers,
      defaultProvider,
      coverProviders,
      defaultCoverProvider,
      seriesSuggestions,
    )
  }

  private fun mapToUiState(
    item: LibraryItem,
    providers: List<MatchProvider>,
    defaultProvider: String,
    coverProviders: List<MatchProvider> = emptyList(),
    defaultCoverProvider: String = "best",
    seriesSuggestions: List<String> = emptyList(),
  ): EditItemUiState {
    val mappedMedia = EditItemMapper.mapMedia(item)
    val details = mappedMedia.details

    return EditItemUiState(
      state = GenericState.Success,
      itemId = item.id,
      mediaKind = mappedMedia.mediaKind,
      coverUrl = helper.generateItemCoverUrl(item.id, item.updatedAt),
      webBaseUrl = "https://${DataStoreManager.BASE_URL}",
      details = details,
      originalDetails = details,
      chapters = mappedMedia.chapters,
      libraryFiles =
        item.libraryFiles.map {
          LibraryFileRow(
            ino = it.ino,
            path = it.metadata.path,
            filename = it.metadata.filename,
            sizeText = helper.humanReadableFileSize(it.metadata.size.toLong()),
            fileType = it.fileType,
          )
        },
      match =
        MatchState(
          providers = providers,
          selectedProvider = defaultProvider,
          title = details.title,
          author = details.primaryAuthor(mappedMedia.mediaKind),
        ),
      coverSearch =
        CoverSearchState(
          provider = defaultCoverProvider,
          providers = coverProviders,
          title = details.title,
          author = details.primaryAuthor(mappedMedia.mediaKind),
        ),
      seriesSuggestions = seriesSuggestions,
    )
  }

  suspend fun save(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val request = buildUpdateRequest(state.mediaKind, state.originalDetails, state.details)
    api.updateItemMedia(state.itemId, request).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return state.copy(isSaving = false)
    }
    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        return state.copy(isSaving = false)
      }
    libraryItemRepo.updateItem(updated)
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return mergeUpdated(state, updated).copy(isSaving = false)
  }

  private fun buildUpdateRequest(
    mediaKind: EditItemMediaKind,
    original: DetailsForm,
    current: DetailsForm,
  ): UpdateLibraryItemMediaRequest = EditItemMapper.buildUpdateRequest(mediaKind, original, current)

  private fun mergeUpdated(state: EditItemUiState, item: LibraryItem): EditItemUiState {
    val updatedState =
      mapToUiState(
        item,
        state.match.providers,
        state.match.selectedProvider,
        state.coverSearch.providers,
        state.coverSearch.provider,
        state.seriesSuggestions,
      )
    return updatedState.copy(
      currentTab = state.currentTab,
      coverSearch =
        state.coverSearch.copy(
          title = updatedState.coverSearch.title,
          author = updatedState.coverSearch.author,
          provider = updatedState.coverSearch.provider,
          providers = updatedState.coverSearch.providers,
        ),
      match =
        state.match.copy(
          providers = updatedState.match.providers,
          selectedProvider = updatedState.match.selectedProvider,
        ),
    )
  }

  suspend fun quickMatch(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val request =
      MatchLibraryItemRequest(
        provider = state.match.selectedProvider,
        title = state.details.title,
        author = state.details.authors.joinToString().ifBlank { null },
      )
    val result =
      api.matchItem(state.itemId, request).getOrElse {
        events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
        return state.copy(isSaving = false)
      }
    return when (result) {
      is MatchItemResult.Warning -> {
        events.emit(GenericUiEvent.ShowErrorSnackbar(result.message))
        state.copy(isSaving = false)
      }
      is MatchItemResult.Success -> {
        libraryItemRepo.updateItem(result.libraryItem)
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        mergeUpdated(state, result.libraryItem).copy(isSaving = false)
      }
    }
  }

  suspend fun reScan(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    api.reScanItem(state.itemId).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return state.copy(isSaving = false)
    }
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return state.copy(isSaving = false)
  }

  suspend fun uploadCover(
    state: EditItemUiState,
    uri: Uri,
    contentResolver: ContentResolver,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
    if (bytes == null) {
      events.emit(GenericUiEvent.ShowErrorSnackbar("Cannot read image"))
      return state.copy(isCoverWorking = false)
    }
    val mime = contentResolver.getType(uri) ?: "image/*"
    val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
    val filename = resolveCoverFilename(contentResolver.getFilename(uri), mime)
    val part = MultipartBody.Part.createFormData("cover", filename, body)
    val response =
      api.uploadItemCover(state.itemId, part).getOrElse {
        events.emit(
          GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty().ifBlank { "Cover upload failed" })
        )
        return state.copy(isCoverWorking = false)
      }
    if (!response.success) {
      events.emit(GenericUiEvent.ShowErrorSnackbar("Cover upload failed"))
      return state.copy(isCoverWorking = false)
    }
    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        return state.copy(isCoverWorking = false)
      }
    libraryItemRepo.updateItem(updated)
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return mergeUpdated(state, updated).copy(isCoverWorking = false)
  }

  suspend fun setCoverUrl(
    state: EditItemUiState,
    url: String,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val response =
      api.setItemCoverFromUrl(state.itemId, CoverFromUrlRequest(url)).getOrElse {
        events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
        return state.copy(isCoverWorking = false)
      }
    if (!response.success) {
      events.emit(GenericUiEvent.ShowErrorSnackbar())
      return state.copy(isCoverWorking = false)
    }
    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        return state.copy(isCoverWorking = false)
      }
    libraryItemRepo.updateItem(updated)
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return mergeUpdated(state, updated).copy(isCoverWorking = false)
  }

  suspend fun deleteCover(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    api.deleteItemCover(state.itemId).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return state.copy(isCoverWorking = false)
    }
    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        return state.copy(isCoverWorking = false)
      }
    libraryItemRepo.updateItem(updated)
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return mergeUpdated(state, updated).copy(isCoverWorking = false)
  }

  suspend fun deleteFile(
    state: EditItemUiState,
    ino: String,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    api.deleteItemFile(state.itemId, ino).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message ?: "Failed to delete file"))
      return state.copy(activeFileActionIno = null, pendingDeleteFile = null)
    }

    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar("File deleted"))
        return state.copy(activeFileActionIno = null, pendingDeleteFile = null)
      }
    libraryItemRepo.updateItem(updated)
    events.emit(GenericUiEvent.ShowSuccessSnackbar("File deleted"))
    return mergeUpdated(state, updated).copy(activeFileActionIno = null, pendingDeleteFile = null)
  }

  suspend fun searchMatches(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val raw =
      api
        .searchBooks(
          provider = state.match.selectedProvider,
          title = state.match.title,
          author = state.match.author.ifBlank { null },
        )
        .getOrElse {
          events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
          return state.copy(match = state.match.copy(isSearching = false))
        }
    val rows = raw.map {
      MatchResultRow(
        cover = it.cover.orEmpty(),
        title = it.title.orEmpty(),
        author = it.author.orEmpty(),
        description = it.description.orEmpty(),
      )
    }
    return state.copy(
      match = state.match.copy(results = rows, rawResults = raw, isSearching = false)
    )
  }

  fun applyMatch(state: EditItemUiState, index: Int): EditItemUiState {
    val result = state.match.rawResults.getOrNull(index) ?: return state
    val newDetails =
      state.details.copy(
        title = result.title ?: state.details.title,
        subtitle = result.subtitle ?: state.details.subtitle,
        authors =
          result.author?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: state.details.authors,
        narrators =
          result.narrator?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: state.details.narrators,
        publisher = result.publisher ?: state.details.publisher,
        publishedYear = result.publishedYear ?: state.details.publishedYear,
        description = result.description ?: state.details.description,
        isbn = result.isbn ?: state.details.isbn,
        asin = result.asin ?: state.details.asin,
        genres = result.genres.ifEmpty { state.details.genres },
        tags = result.tags.ifEmpty { state.details.tags },
        series =
          if (result.series.isNotEmpty())
            result.series.mapNotNull { ref ->
              ref.series?.let { SeriesEntry(it, ref.sequence.orEmpty()) }
            }
          else state.details.series,
      )
    return state.copy(details = newDetails, currentTab = EditItemTab.Details)
  }

  suspend fun searchCovers(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val response =
      api
        .searchCovers(
          title = state.coverSearch.title,
          author = state.coverSearch.author.ifBlank { null },
          provider = state.coverSearch.provider,
          podcast = coverSearchPodcastFlag(state.mediaKind),
        )
        .getOrElse {
          events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
          return state.copy(coverSearch = state.coverSearch.copy(state = GenericState.Success))
        }
    return state.copy(
      coverSearch = state.coverSearch.copy(results = response.results, state = GenericState.Success)
    )
  }

  suspend fun embedMetadata(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    api.embedItemMetadata(state.itemId).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return state.copy(isToolWorking = false)
    }
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return state.copy(isToolWorking = false)
  }
}

internal fun matchProvidersFor(
  providers: SearchProviders?,
  mediaKind: EditItemMediaKind,
): List<MatchProvider> =
  when (mediaKind) {
      EditItemMediaKind.Book -> providers?.books
      EditItemMediaKind.Podcast -> providers?.podcasts
    }
    ?.map { MatchProvider(it.value, it.text) }
    .orEmpty()

internal fun coverProvidersFor(
  providers: SearchProviders?,
  mediaKind: EditItemMediaKind,
): List<MatchProvider> =
  when (mediaKind) {
      EditItemMediaKind.Book -> providers?.booksCovers
      EditItemMediaKind.Podcast -> providers?.podcasts
    }
    ?.map { MatchProvider(it.value, it.text) }
    .orEmpty()

internal fun coverSearchPodcastFlag(mediaKind: EditItemMediaKind): Int =
  when (mediaKind) {
    EditItemMediaKind.Book -> 0
    EditItemMediaKind.Podcast -> 1
  }

internal fun resolveCoverFilename(filename: String, mime: String): String {
  val sanitized = filename.substringAfterLast('/').trim().ifBlank { "cover" }
  if (sanitized.substringAfterLast('.', "").isNotBlank()) return sanitized

  val extension =
    when (mime.lowercase()) {
      "image/jpeg",
      "image/jpg" -> "jpg"
      "image/png" -> "png"
      "image/webp" -> "webp"
      "image/gif" -> "gif"
      else -> "jpg"
    }
  return "$sanitized.$extension"
}
