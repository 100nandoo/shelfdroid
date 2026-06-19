package dev.halim.shelfdroid.core.data.screen.edititem

import android.content.ContentResolver
import android.net.Uri
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CoverFromUrlRequest
import dev.halim.core.network.request.MatchLibraryItemRequest
import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.MatchItemResult
import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.core.network.response.SearchPodcast
import dev.halim.core.network.response.SearchProviders
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.helper.formatFileSize
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
  private val episodeUpdateRunner =
    EditItemEpisodeUpdateRunner(
      updateEpisodeCutoff = { itemId, request -> api.updateItemMedia(itemId, request) },
      checkNewEpisodes = { itemId, limit ->
        api.checkNewEpisodes(itemId, limit).map { it.episodes.size }
      },
      reloadItem = { itemId -> api.item(itemId) },
      mergeUpdated = ::mergeUpdated,
      updateCachedItem = libraryItemRepo::updateItem,
    )

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
    val defaultProvider =
      providers.firstOrNull()?.value
        ?: when (mediaKind) {
          EditItemMediaKind.Book -> DEFAULT_BOOK_MATCH_PROVIDER
          EditItemMediaKind.Podcast -> DEFAULT_PODCAST_MATCH_PROVIDER
        }
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
      episodes = mappedMedia.episodes,
      episodeUpdate = mappedMedia.episodeUpdate,
      libraryFiles =
        item.libraryFiles.map {
          LibraryFileRow(
            ino = it.ino,
            path = it.metadata.path,
            filename = it.metadata.filename,
            sizeText = it.metadata.size.toLong().formatFileSize(),
            fileType = it.fileType,
          )
        },
      match = createMatchState(mappedMedia.mediaKind, providers, defaultProvider, details),
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
    persistPendingCover(state, events)?.let {
      return it
    }
    val updated =
      api.item(state.itemId).getOrElse {
        events.emit(GenericUiEvent.ShowSuccessSnackbar())
        return state.copy(isSaving = false, pendingCoverUrl = null)
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
      episodeUpdate =
        updatedState.episodeUpdate.copy(
          limitInput = state.episodeUpdate.limitInput,
          isRunning = false,
        ),
      coverSearch =
        state.coverSearch.copy(
          title = updatedState.coverSearch.title,
          author = updatedState.coverSearch.author,
          provider = updatedState.coverSearch.provider,
          providers = updatedState.coverSearch.providers,
        ),
      match = mergeMatchState(state.match, updatedState.match),
      pendingCoverUrl = null,
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

  suspend fun runEpisodeUpdateCheck(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val result = episodeUpdateRunner.run(state)
    result.events.forEach { events.emit(it) }
    return result.state
  }

  suspend fun searchMatches(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    return when (val match = state.match) {
      is MatchState.Book -> {
        val raw =
          api
            .searchBooks(
              provider = match.selectedProvider,
              title = match.title,
              author = match.author.ifBlank { null },
            )
            .getOrElse {
              events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
              return state.copy(match = match.copy(isSearching = false))
            }
        state.copy(
          match =
            match.copy(
              results = mapBookMatchRows(raw),
              rawResults = raw,
              isSearching = false,
            )
        )
      }
      is MatchState.Podcast -> {
        val raw =
          api.searchPodcast(term = match.searchTerm, provider = match.selectedProvider).getOrElse {
            events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
            return state.copy(match = match.copy(isSearching = false))
          }
        state.copy(
          match =
            match.copy(
              results = mapPodcastMatchRows(raw),
              review = null,
              isSearching = false,
            )
        )
      }
    }
  }

  fun applyBookMatch(state: EditItemUiState, index: Int): EditItemUiState {
    val match = state.match as? MatchState.Book ?: return state
    val result = match.rawResults.getOrNull(index) ?: return state
    return state.copy(
      details = applyBookMatchResult(state.details, result),
      currentTab = EditItemTab.Details,
    )
  }

  fun updateBookMatch(
    state: EditItemUiState,
    transform: (MatchState.Book) -> MatchState.Book,
  ): EditItemUiState {
    val match = state.match as? MatchState.Book ?: return state
    return state.copy(match = transform(match))
  }

  fun updatePodcastMatch(
    state: EditItemUiState,
    transform: (MatchState.Podcast) -> MatchState.Podcast,
  ): EditItemUiState {
    val match = state.match as? MatchState.Podcast ?: return state
    return state.copy(match = transform(match))
  }

  fun openPodcastMatchReview(state: EditItemUiState, index: Int): EditItemUiState {
    val match = state.match as? MatchState.Podcast ?: return state
    val result = match.results.getOrNull(index) ?: return state
    return state.copy(
      match =
        match.copy(
          review =
            PodcastMatchReviewState(
              result = result,
              draft =
                PodcastMatchDraft(
                  title = result.title,
                  author = result.author,
                  feedUrl = result.feedUrl,
                  itunesId = result.itunesId,
                  releaseDate = result.releaseDate,
                  explicit = result.explicit,
                ),
              selectedFields = defaultPodcastMatchFields(result),
            )
        )
    )
  }

  fun dismissPodcastMatchReview(state: EditItemUiState): EditItemUiState {
    val match = state.match as? MatchState.Podcast ?: return state
    return state.copy(match = match.copy(review = null))
  }

  fun updatePodcastMatchReview(
    state: EditItemUiState,
    transform: (PodcastMatchReviewState) -> PodcastMatchReviewState,
  ): EditItemUiState {
    val match = state.match as? MatchState.Podcast ?: return state
    val review = match.review ?: return state
    return state.copy(match = match.copy(review = transform(review)))
  }

  fun updatePodcastMatchDraft(
    state: EditItemUiState,
    transform: (PodcastMatchDraft) -> PodcastMatchDraft,
  ): EditItemUiState =
    updatePodcastMatchReview(state) { review ->
      review.copy(draft = transform(review.draft))
    }

  fun togglePodcastMatchField(state: EditItemUiState, field: PodcastMatchField): EditItemUiState =
    updatePodcastMatchReview(state) { review ->
      val selectedFields =
        if (field in review.selectedFields) review.selectedFields - field
        else review.selectedFields + field
      review.copy(selectedFields = selectedFields)
    }

  fun applyPodcastMatchReview(state: EditItemUiState): EditItemUiState {
    val match = state.match as? MatchState.Podcast ?: return state
    val review = match.review ?: return state
    return applyPodcastMatchSelection(state, review)
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

  private suspend fun persistPendingCover(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState? {
    val pendingCoverUrl = state.pendingCoverUrl?.takeIf { it.isNotBlank() } ?: return null
    val response =
      api.setItemCoverFromUrl(state.itemId, CoverFromUrlRequest(pendingCoverUrl)).getOrElse {
        events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
        return state.copy(isSaving = false)
      }
    if (!response.success) {
      events.emit(GenericUiEvent.ShowErrorSnackbar())
      return state.copy(isSaving = false)
    }
    return null
  }
}

private fun createMatchState(
  mediaKind: EditItemMediaKind,
  providers: List<MatchProvider>,
  selectedProvider: String,
  details: DetailsForm,
): MatchState =
  when (mediaKind) {
    EditItemMediaKind.Book ->
      MatchState.Book(
        providers = providers,
        selectedProvider = selectedProvider,
        title = details.title,
        author = details.primaryAuthor(mediaKind),
      )
    EditItemMediaKind.Podcast ->
      MatchState.Podcast(
        providers = providers,
        selectedProvider = selectedProvider,
        searchTerm = details.title,
      )
  }

private fun mergeMatchState(current: MatchState, updated: MatchState): MatchState =
  when {
    current is MatchState.Book && updated is MatchState.Book ->
      current.copy(
        providers = updated.providers,
        selectedProvider = current.selectedProvider,
      )
    current is MatchState.Podcast && updated is MatchState.Podcast ->
      current.copy(
        providers = updated.providers,
        selectedProvider = current.selectedProvider,
      )
    else -> updated
  }

internal fun mapBookMatchRows(raw: List<SearchBookMatchResponse>): List<MatchResultRow> = raw.map {
  MatchResultRow(
    cover = it.cover.orEmpty(),
    title = it.title.orEmpty(),
    author = it.author.orEmpty(),
    description = it.description.orEmpty(),
  )
}

internal fun mapPodcastMatchRows(raw: List<SearchPodcast>): List<PodcastMatchResultRow> = raw.map {
  PodcastMatchResultRow(
    cover = it.cover,
    title = it.title,
    author = it.artistName,
    genres = it.genres,
    episodeCount = it.trackCount,
    feedUrl = it.feedUrl,
    itunesId = it.id.toString(),
    releaseDate = it.releaseDate,
    explicit = it.explicit,
    description = it.descriptionPlain.ifBlank { it.description },
  )
}

internal fun applyBookMatchResult(
  details: DetailsForm,
  result: SearchBookMatchResponse,
): DetailsForm =
  details.copy(
    title = result.title ?: details.title,
    subtitle = result.subtitle ?: details.subtitle,
    authors =
      result.author?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: details.authors,
    narrators =
      result.narrator?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: details.narrators,
    publisher = result.publisher ?: details.publisher,
    publishedYear = result.publishedYear ?: details.publishedYear,
    description = result.description ?: details.description,
    isbn = result.isbn ?: details.isbn,
    asin = result.asin ?: details.asin,
    genres = result.genres.ifEmpty { details.genres },
    tags = result.tags.ifEmpty { details.tags },
    series =
      if (result.series.isNotEmpty())
        result.series.mapNotNull { ref ->
          ref.series?.let { SeriesEntry(it, ref.sequence.orEmpty()) }
        }
      else details.series,
  )

internal fun defaultPodcastMatchFields(result: PodcastMatchResultRow): Set<PodcastMatchField> =
  buildSet {
    if (result.cover.isNotBlank()) add(PodcastMatchField.Cover)
    if (result.title.isNotBlank()) add(PodcastMatchField.Title)
    if (result.author.isNotBlank()) add(PodcastMatchField.Author)
    if (result.genres.isNotEmpty()) add(PodcastMatchField.Genres)
    if (result.feedUrl.isNotBlank()) add(PodcastMatchField.RssFeedUrl)
    if (result.itunesId.isNotBlank()) add(PodcastMatchField.ItunesId)
    if (result.releaseDate.isNotBlank()) add(PodcastMatchField.ReleaseDate)
    add(PodcastMatchField.Explicit)
  }

internal fun applyPodcastReviewToState(
  state: EditItemUiState,
  review: PodcastMatchReviewState,
): EditItemUiState {
  val result = review.result
  val draft = review.draft
  val selected = review.selectedFields
  return state.copy(
    details =
      state.details.copy(
        title = draft.title.takeIf { PodcastMatchField.Title in selected } ?: state.details.title,
        podcastAuthor =
          draft.author.takeIf { PodcastMatchField.Author in selected }
            ?: state.details.podcastAuthor,
        genres =
          result.genres.takeIf { PodcastMatchField.Genres in selected } ?: state.details.genres,
        rssFeedUrl =
          draft.feedUrl.takeIf { PodcastMatchField.RssFeedUrl in selected }
            ?: state.details.rssFeedUrl,
        itunesId =
          draft.itunesId.takeIf { PodcastMatchField.ItunesId in selected }
            ?: state.details.itunesId,
        releaseDate =
          draft.releaseDate.takeIf { PodcastMatchField.ReleaseDate in selected }
            ?: state.details.releaseDate,
        explicit =
          if (PodcastMatchField.Explicit in selected) draft.explicit else state.details.explicit,
      ),
    pendingCoverUrl =
      result.cover.takeIf { PodcastMatchField.Cover in selected && it.isNotBlank() }
        ?: state.pendingCoverUrl,
  )
}

internal fun applyPodcastMatchSelection(
  state: EditItemUiState,
  review: PodcastMatchReviewState,
): EditItemUiState {
  val match = state.match as? MatchState.Podcast ?: return state
  return applyPodcastReviewToState(state, review)
    .copy(
      currentTab = EditItemTab.Details,
      match = match.copy(review = null),
    )
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
