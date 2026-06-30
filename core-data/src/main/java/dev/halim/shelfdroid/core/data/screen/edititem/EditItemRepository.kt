package dev.halim.shelfdroid.core.data.screen.edititem

import android.content.ContentResolver
import android.net.Uri
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CoverFromUrlRequest
import dev.halim.core.network.request.MatchLibraryItemRequest
import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.request.ValidateCronRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.MatchItemResult
import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.core.network.response.SearchPodcast
import dev.halim.core.network.response.SearchProviders
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.screen.edititem.episodes.EditItemEpisodeUpdateRunner
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.EditItemScheduleSaveRunner
import dev.halim.shelfdroid.core.data.screen.edititem.schedule.deriveSchedulePresentation
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
  private fun currentWebBaseUrl(): String =
    AudiobookshelfBaseUrl.parse(DataStoreManager.BASE_URL)?.value
      ?: AudiobookshelfBaseUrl.DEFAULT_VALUE

  private val episodeUpdateRunner =
    EditItemEpisodeUpdateRunner(
      updateEpisodeCutoff = { itemId, request -> api.updateItemMedia(itemId, request).map {} },
      checkNewEpisodes = { itemId, limit ->
        api.checkNewEpisodes(itemId, limit).map { it.episodes.size }
      },
      reloadItem = { itemId -> api.item(itemId) },
      mergeUpdated = ::mergeUpdated,
      updateCachedItem = libraryItemRepo::updateItem,
    )
  private val scheduleSaveRunner =
    EditItemScheduleSaveRunner(
      validateCron = { expression -> api.validateCron(ValidateCronRequest(expression)) },
      updateSchedule = api::updateItemMedia,
      updateCachedItem = libraryItemRepo::updateItem,
    )

  suspend fun load(itemId: String): EditItemUiState {
    val item =
      api.item(itemId).getOrElse {
        return EditItemUiState(
          state = GenericState.Failure(it.message),
          itemId = itemId,
          coverUrl = helper.generateItemCoverUrl(itemId),
          webBaseUrl = currentWebBaseUrl(),
        )
      }

    val mediaKind = EditItemMapper.mapMedia(item).mediaKind
    val providersResponse = api.searchProviders().getOrNull()?.providers
    val providers = matchProvidersFor(providersResponse, mediaKind)
    val coverProviders = coverProvidersFor(providersResponse, mediaKind)
    val defaultProvider = defaultMatchProviderFor(providers, mediaKind)
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
    val schedulePresentation = deriveSchedulePresentation(mappedMedia.schedule)

    return EditItemUiState(
      state = GenericState.Success,
      itemId = item.id,
      mediaKind = mappedMedia.mediaKind,
      coverUrl = helper.generateItemCoverUrl(item.id, item.updatedAt),
      webBaseUrl = currentWebBaseUrl(),
      details = details,
      originalDetails = details,
      schedule = mappedMedia.schedule,
      originalSchedule = mappedMedia.schedule,
      scheduleMode = schedulePresentation.mode,
      simpleScheduleBuilder = schedulePresentation.simpleBuilder,
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

  suspend fun saveSchedule(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val result = scheduleSaveRunner.run(state)
    result.events.forEach { events.emit(it) }
    return result.state
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
    val schedulePresentation =
      deriveSchedulePresentation(
        updatedState.schedule,
        preferredMode = state.scheduleMode,
        currentBuilder = state.simpleScheduleBuilder,
      )
    return updatedState.copy(
      currentTab = state.currentTab,
      scheduleMode = schedulePresentation.mode,
      simpleScheduleBuilder = schedulePresentation.simpleBuilder,
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
              fallbackTitleOnly = 1,
              id = state.itemId.ifBlank { null },
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
              review = null,
              hasSearched = true,
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
              hasSearched = true,
              isSearching = false,
            )
        )
      }
    }
  }

  fun openBookMatchReview(state: EditItemUiState, index: Int): EditItemUiState {
    val match = state.match as? MatchState.Book ?: return state
    val result = match.rawResults.getOrNull(index)?.toBookMatchReviewResult() ?: return state
    return state.copy(
      match =
        match.copy(
          review =
            BookMatchReviewState(
              result = result,
              draft = result.toBookMatchDraft(),
              selectedFields = defaultBookMatchFields(result),
            )
        )
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

  fun dismissBookMatchReview(state: EditItemUiState): EditItemUiState {
    val match = state.match as? MatchState.Book ?: return state
    return state.copy(match = match.copy(review = null))
  }

  fun updateBookMatchReview(
    state: EditItemUiState,
    transform: (BookMatchReviewState) -> BookMatchReviewState,
  ): EditItemUiState {
    val match = state.match as? MatchState.Book ?: return state
    val review = match.review ?: return state
    return state.copy(match = match.copy(review = transform(review)))
  }

  fun updateBookMatchDraft(
    state: EditItemUiState,
    transform: (BookMatchDraft) -> BookMatchDraft,
  ): EditItemUiState =
    updateBookMatchReview(state) { review ->
      review.copy(draft = transform(review.draft))
    }

  fun toggleBookMatchField(state: EditItemUiState, field: BookMatchField): EditItemUiState =
    updateBookMatchReview(state) { review ->
      val selectedFields =
        if (field in review.selectedFields) review.selectedFields - field
        else review.selectedFields + field
      review.copy(selectedFields = selectedFields)
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

  suspend fun applyPodcastMatchReview(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val match = state.match as? MatchState.Podcast ?: return state.copy(isSaving = false)
    val review = match.review ?: return state.copy(isSaving = false)
    if (review.selectedFields.isEmpty()) return state.copy(isSaving = false)

    val request = buildPodcastMatchReviewUpdateRequest(state, review)
    if (request.isEmpty()) {
      val appliedState =
        applyPodcastMatchSelection(
          state.copy(details = state.originalDetails, pendingCoverUrl = null),
          review,
        )
      events.emit(GenericUiEvent.ShowSuccessSnackbar())
      return appliedState.copy(
        originalDetails = appliedState.details,
        isSaving = false,
      )
    }

    val response =
      api.updateItemMedia(state.itemId, request).getOrElse {
        events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
        return state.copy(isSaving = false)
      }

    val updated = response.libraryItem
    if (updated != null) {
      libraryItemRepo.updateItem(updated)
      events.emit(GenericUiEvent.ShowSuccessSnackbar())
      return mergeUpdated(
          dismissPodcastMatchReview(state).copy(currentTab = EditItemTab.Details),
          updated,
        )
        .copy(isSaving = false, currentTab = EditItemTab.Details)
    }

    val appliedState =
      applyPodcastMatchSelection(
        state.copy(details = state.originalDetails, pendingCoverUrl = null),
        review,
      )
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return appliedState.copy(
      originalDetails = appliedState.details,
      isSaving = false,
    )
  }

  suspend fun applyBookMatchReview(
    state: EditItemUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditItemUiState {
    val match = state.match as? MatchState.Book ?: return state.copy(isSaving = false)
    val review = match.review ?: return state.copy(isSaving = false)
    if (review.selectedFields.isEmpty()) return state.copy(isSaving = false)

    val request = buildBookMatchReviewUpdateRequest(state, review)
    if (request.isEmpty()) {
      val appliedState =
        applyBookMatchSelection(
          state.copy(details = state.originalDetails, pendingCoverUrl = null),
          review,
        )
      events.emit(GenericUiEvent.ShowSuccessSnackbar())
      return appliedState.copy(
        originalDetails = appliedState.details,
        isSaving = false,
      )
    }

    val response =
      api.updateItemMedia(state.itemId, request).getOrElse {
        events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
        return state.copy(isSaving = false)
      }

    val updated = response.libraryItem
    if (updated != null) {
      libraryItemRepo.updateItem(updated)
      events.emit(GenericUiEvent.ShowSuccessSnackbar())
      return mergeUpdated(
          dismissBookMatchReview(state).copy(currentTab = EditItemTab.Details),
          updated,
        )
        .copy(isSaving = false, currentTab = EditItemTab.Details)
    }

    val appliedState =
      applyBookMatchSelection(
        state.copy(details = state.originalDetails, pendingCoverUrl = null),
        review,
      )
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    return appliedState.copy(
      originalDetails = appliedState.details,
      isSaving = false,
    )
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

private fun SearchBookMatchResponse.toBookMatchReviewResult() =
  BookMatchReviewResult(
    cover = cover.orEmpty(),
    title = title.orEmpty(),
    subtitle = subtitle.orEmpty(),
    authors = author.orEmpty().splitMatchNames(),
    narrators = narrator.orEmpty().splitMatchNames(),
    publisher = publisher.orEmpty(),
    publishedYear = publishedYear.orEmpty(),
    description = description.orEmpty(),
    isbn = isbn.orEmpty(),
    asin = asin.orEmpty(),
    abridged = abridged,
    genres = genres,
    tags = tags,
    series =
      series.mapNotNull { ref ->
        ref.series?.let { SeriesEntry(it, ref.sequence.orEmpty()) }
      },
  )

private fun BookMatchReviewResult.toBookMatchDraft() =
  BookMatchDraft(
    title = title,
    subtitle = subtitle,
    authors = authors,
    narrators = narrators,
    publisher = publisher,
    publishedYear = publishedYear,
    description = description,
    isbn = isbn,
    asin = asin,
    abridged = abridged ?: false,
    genres = genres,
    tags = tags,
    series = series,
  )

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
    abridged = result.abridged ?: details.abridged,
    genres = result.genres.ifEmpty { details.genres },
    tags = result.tags.ifEmpty { details.tags },
    series =
      if (result.series.isNotEmpty())
        result.series.mapNotNull { ref ->
          ref.series?.let { SeriesEntry(it, ref.sequence.orEmpty()) }
        }
      else details.series,
  )

internal fun defaultBookMatchFields(result: BookMatchReviewResult): Set<BookMatchField> = buildSet {
  if (result.cover.isNotBlank()) add(BookMatchField.Cover)
  if (result.title.isNotBlank()) add(BookMatchField.Title)
  if (result.subtitle.isNotBlank()) add(BookMatchField.Subtitle)
  if (result.authors.isNotEmpty()) add(BookMatchField.Authors)
  if (result.narrators.isNotEmpty()) add(BookMatchField.Narrators)
  if (result.publisher.isNotBlank()) add(BookMatchField.Publisher)
  if (result.publishedYear.isNotBlank()) add(BookMatchField.PublishedYear)
  if (result.description.isNotBlank()) add(BookMatchField.Description)
  if (result.isbn.isNotBlank()) add(BookMatchField.Isbn)
  if (result.asin.isNotBlank()) add(BookMatchField.Asin)
  if (result.abridged != null) add(BookMatchField.Abridged)
  if (result.genres.isNotEmpty()) add(BookMatchField.Genres)
  if (result.tags.isNotEmpty()) add(BookMatchField.Tags)
  if (result.series.isNotEmpty()) add(BookMatchField.Series)
}

internal fun applyBookReviewToState(
  state: EditItemUiState,
  review: BookMatchReviewState,
): EditItemUiState {
  val draft = review.draft
  val selected = review.selectedFields
  return state.copy(
    details =
      state.details.copy(
        title = draft.title.takeIf { BookMatchField.Title in selected } ?: state.details.title,
        subtitle =
          draft.subtitle.takeIf { BookMatchField.Subtitle in selected } ?: state.details.subtitle,
        authors =
          draft.authors.takeIf { BookMatchField.Authors in selected } ?: state.details.authors,
        narrators =
          draft.narrators.takeIf { BookMatchField.Narrators in selected }
            ?: state.details.narrators,
        publisher =
          draft.publisher.takeIf { BookMatchField.Publisher in selected }
            ?: state.details.publisher,
        publishedYear =
          draft.publishedYear.takeIf { BookMatchField.PublishedYear in selected }
            ?: state.details.publishedYear,
        description =
          draft.description.takeIf { BookMatchField.Description in selected }
            ?: state.details.description,
        isbn = draft.isbn.takeIf { BookMatchField.Isbn in selected } ?: state.details.isbn,
        asin = draft.asin.takeIf { BookMatchField.Asin in selected } ?: state.details.asin,
        abridged =
          if (BookMatchField.Abridged in selected) draft.abridged else state.details.abridged,
        genres = draft.genres.takeIf { BookMatchField.Genres in selected } ?: state.details.genres,
        tags = draft.tags.takeIf { BookMatchField.Tags in selected } ?: state.details.tags,
        series = draft.series.takeIf { BookMatchField.Series in selected } ?: state.details.series,
      ),
    pendingCoverUrl =
      review.result.cover.takeIf { BookMatchField.Cover in selected && it.isNotBlank() }
        ?: state.pendingCoverUrl,
  )
}

internal fun buildBookMatchReviewUpdateRequest(
  state: EditItemUiState,
  review: BookMatchReviewState,
): UpdateLibraryItemMediaRequest {
  val selectedOnlyState =
    applyBookReviewToState(
      state.copy(details = state.originalDetails, pendingCoverUrl = null),
      review,
    )
  val metadata =
    UpdateLibraryItemMediaRequest.Metadata(
      title =
        selectedOnlyState.details.title.takeIf { BookMatchField.Title in review.selectedFields },
      subtitle =
        selectedOnlyState.details.subtitle.takeIf {
          BookMatchField.Subtitle in review.selectedFields
        },
      authors =
        selectedOnlyState.details.authors
          .takeIf { BookMatchField.Authors in review.selectedFields }
          ?.map(UpdateLibraryItemMediaRequest::NameRef),
      narrators =
        selectedOnlyState.details.narrators.takeIf {
          BookMatchField.Narrators in review.selectedFields
        },
      series =
        selectedOnlyState.details.series
          .takeIf { BookMatchField.Series in review.selectedFields }
          ?.map { UpdateLibraryItemMediaRequest.SeriesRef(it.name, it.sequence.ifBlank { null }) },
      genres =
        selectedOnlyState.details.genres.takeIf { BookMatchField.Genres in review.selectedFields },
      publishedYear =
        selectedOnlyState.details.publishedYear.takeIf {
          BookMatchField.PublishedYear in review.selectedFields
        },
      publisher =
        selectedOnlyState.details.publisher.takeIf {
          BookMatchField.Publisher in review.selectedFields
        },
      description =
        selectedOnlyState.details.description.takeIf {
          BookMatchField.Description in review.selectedFields
        },
      isbn = selectedOnlyState.details.isbn.takeIf { BookMatchField.Isbn in review.selectedFields },
      asin = selectedOnlyState.details.asin.takeIf { BookMatchField.Asin in review.selectedFields },
      abridged =
        selectedOnlyState.details.abridged.takeIf {
          BookMatchField.Abridged in review.selectedFields
        },
    )
  return UpdateLibraryItemMediaRequest(
    metadata = metadata.takeUnless { it.isEmpty() },
    tags = selectedOnlyState.details.tags.takeIf { BookMatchField.Tags in review.selectedFields },
    url = selectedOnlyState.pendingCoverUrl,
  )
}

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

internal fun buildPodcastMatchReviewUpdateRequest(
  state: EditItemUiState,
  review: PodcastMatchReviewState,
): UpdateLibraryItemMediaRequest {
  val selectedOnlyState =
    applyPodcastReviewToState(
      state.copy(details = state.originalDetails, pendingCoverUrl = null),
      review,
    )
  val metadata =
    UpdateLibraryItemMediaRequest.Metadata(
      title =
        selectedOnlyState.details.title.takeIf { PodcastMatchField.Title in review.selectedFields },
      author =
        selectedOnlyState.details.podcastAuthor.takeIf {
          PodcastMatchField.Author in review.selectedFields
        },
      genres =
        selectedOnlyState.details.genres.takeIf {
          PodcastMatchField.Genres in review.selectedFields
        },
      releaseDate =
        selectedOnlyState.details.releaseDate.takeIf {
          PodcastMatchField.ReleaseDate in review.selectedFields
        },
      feedUrl =
        selectedOnlyState.details.rssFeedUrl.takeIf {
          PodcastMatchField.RssFeedUrl in review.selectedFields
        },
      itunesId =
        selectedOnlyState.details.itunesId
          .takeIf { PodcastMatchField.ItunesId in review.selectedFields }
          ?.trim()
          ?.toIntOrNull(),
      explicit =
        selectedOnlyState.details.explicit.takeIf {
          PodcastMatchField.Explicit in review.selectedFields
        },
    )
  return UpdateLibraryItemMediaRequest(
    metadata = metadata.takeUnless { it.isEmpty() },
    url = selectedOnlyState.pendingCoverUrl,
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

internal fun applyBookMatchSelection(
  state: EditItemUiState,
  review: BookMatchReviewState,
): EditItemUiState {
  val match = state.match as? MatchState.Book ?: return state
  return applyBookReviewToState(state, review)
    .copy(
      currentTab = EditItemTab.Details,
      match = match.copy(review = null),
    )
}

private fun UpdateLibraryItemMediaRequest.isEmpty(): Boolean =
  metadata == null && tags == null && url == null && lastEpisodeCheck == null

private fun String.splitMatchNames(): List<String> =
  split(",").map { it.trim() }.filter { it.isNotEmpty() }

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

internal fun defaultMatchProviderFor(
  providers: List<MatchProvider>,
  mediaKind: EditItemMediaKind,
): String {
  val preferredProvider =
    when (mediaKind) {
      EditItemMediaKind.Book -> DEFAULT_BOOK_MATCH_PROVIDER
      EditItemMediaKind.Podcast -> DEFAULT_PODCAST_MATCH_PROVIDER
    }

  return providers.firstOrNull { it.value == preferredProvider }?.value
    ?: providers.firstOrNull()?.value
    ?: preferredProvider
}

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
