package dev.halim.shelfdroid.core.data.screen.edititem

import android.content.ContentResolver
import android.net.Uri
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CoverFromUrlRequest
import dev.halim.core.network.request.MatchLibraryItemRequest
import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.MatchItemResult
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditItemRepository
@Inject
constructor(private val api: ApiService, private val helper: Helper) {

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

    val providersResponse = api.searchProviders().getOrNull()?.providers
    val providers =
      providersResponse?.books?.map { MatchProvider(it.value, it.text) } ?: emptyList()
    val coverProviders =
      providersResponse?.booksCovers?.map { MatchProvider(it.value, it.text) } ?: emptyList()
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
    val media = item.media as? Book
    val metadata = media?.metadata
    val details =
      DetailsForm(
        title = metadata?.title.orEmpty(),
        subtitle = metadata?.subtitle.orEmpty(),
        authors = metadata?.authors?.map { it.name } ?: emptyList(),
        narrators = metadata?.narrators ?: emptyList(),
        series =
          metadata?.series?.map { SeriesEntry(it.name, it.sequence.orEmpty()) } ?: emptyList(),
        genres = metadata?.genres ?: emptyList(),
        tags = media?.tags ?: emptyList(),
        publishedYear = metadata?.publishedYear.orEmpty(),
        publisher = metadata?.publisher.orEmpty(),
        description = metadata?.description.orEmpty(),
        isbn = metadata?.isbn.orEmpty(),
        asin = metadata?.asin.orEmpty(),
        language = metadata?.language.orEmpty(),
        explicit = metadata?.explicit ?: false,
        abridged = false,
      )

    return EditItemUiState(
      state = GenericState.Success,
      itemId = item.id,
      coverUrl = helper.generateItemCoverUrl(item.id),
      webBaseUrl = "https://${DataStoreManager.BASE_URL}",
      details = details,
      originalDetails = details,
      chapters =
        media?.chapters?.map { ChapterRow(it.id, it.title, it.start, it.end) } ?: emptyList(),
      libraryFiles =
        item.libraryFiles.map {
          LibraryFileRow(
            path = it.metadata.path,
            size = it.metadata.size.toLong(),
            fileType = it.fileType,
          )
        },
      match =
        MatchState(
          providers = providers,
          selectedProvider = defaultProvider,
          title = details.title,
          author = details.authors.joinToString(),
        ),
      coverSearch =
        CoverSearchState(
          provider = defaultCoverProvider,
          providers = coverProviders,
          title = details.title,
          author = details.authors.joinToString(),
        ),
      seriesSuggestions = seriesSuggestions,
    )
  }

  suspend fun save(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val request = buildUpdateRequest(state.originalDetails, state.details)
    api.updateItemMedia(state.itemId, request).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return state.copy(isSaving = false)
    }
    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        return state.copy(isSaving = false)
      }
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return mergeUpdated(state, updated).copy(isSaving = false)
  }

  private fun buildUpdateRequest(
    original: DetailsForm,
    current: DetailsForm,
  ): UpdateLibraryItemMediaRequest {
    fun <T> delta(orig: T, cur: T): T? = if (cur != orig) cur else null
    fun deltaNames(
      orig: List<String>,
      cur: List<String>,
    ): List<UpdateLibraryItemMediaRequest.NameRef>? =
      if (cur != orig) cur.map { UpdateLibraryItemMediaRequest.NameRef(it) } else null
    fun deltaSeries(
      orig: List<SeriesEntry>,
      cur: List<SeriesEntry>,
    ): List<UpdateLibraryItemMediaRequest.SeriesRef>? =
      if (cur != orig)
        cur.map { UpdateLibraryItemMediaRequest.SeriesRef(it.name, it.sequence.ifBlank { null }) }
      else null

    return UpdateLibraryItemMediaRequest(
      metadata =
        UpdateLibraryItemMediaRequest.Metadata(
          title = delta(original.title, current.title),
          subtitle = delta(original.subtitle, current.subtitle),
          authors = deltaNames(original.authors, current.authors),
          narrators = delta(original.narrators, current.narrators),
          series = deltaSeries(original.series, current.series),
          genres = delta(original.genres, current.genres),
          publishedYear = delta(original.publishedYear, current.publishedYear),
          publisher = delta(original.publisher, current.publisher),
          description = delta(original.description, current.description),
          isbn = delta(original.isbn, current.isbn),
          asin = delta(original.asin, current.asin),
          language = delta(original.language, current.language),
          explicit = delta(original.explicit, current.explicit),
          abridged = delta(original.abridged, current.abridged),
        ),
      tags = delta(original.tags, current.tags),
    )
  }

  private fun mergeUpdated(state: EditItemUiState, item: LibraryItem): EditItemUiState {
    return mapToUiState(
        item,
        state.match.providers,
        state.match.selectedProvider,
        state.coverSearch.providers,
        state.coverSearch.provider,
        state.seriesSuggestions,
      )
      .copy(currentTab = state.currentTab)
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
    val part = MultipartBody.Part.createFormData("cover", "cover", body)
    val updated =
      api.uploadItemCover(state.itemId, part).getOrElse {
        events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
        return state.copy(isCoverWorking = false)
      }
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
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return state.copy(isCoverWorking = false)
  }

  suspend fun deleteCover(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    api.deleteItemCover(state.itemId).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return state.copy(isCoverWorking = false)
    }
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return state.copy(isCoverWorking = false)
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
