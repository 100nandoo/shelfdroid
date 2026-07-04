package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.request.UpdatePodcastEpisodeRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.SearchPodcastEpisodeMatch
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericUiEvent

data class EditEpisodeMatchResult(
  val state: EditEpisodeUiState,
  val events: List<GenericUiEvent>,
)

class EditEpisodeMatchRunner(
  private val searchEpisodes:
    suspend (itemId: String, title: String) -> Result<List<SearchPodcastEpisodeMatch>>,
  private val updateEpisode:
    suspend (itemId: String, episodeId: String, request: UpdatePodcastEpisodeRequest) -> Result<
        LibraryItem
      >,
  private val updateCachedItem: (LibraryItem) -> Unit = {},
) {
  suspend fun search(state: EditEpisodeUiState): EditEpisodeMatchResult {
    val match = state.match
    val title = match.searchTerm.trim()
    if (title.isBlank()) {
      return EditEpisodeMatchResult(
        state = state.copy(match = match.copy(isSearching = false)),
        events = emptyList(),
      )
    }

    val rawMatches =
      searchEpisodes(state.itemId, title).getOrElse {
        val message = it.message.orEmpty().ifBlank { FAILED_TO_SEARCH_MATCHES }
        return EditEpisodeMatchResult(
          state =
            state.copy(
              match =
                match.copy(
                  searchTerm = title,
                  results = emptyList(),
                  isSearching = false,
                  hasSearched = true,
                  errorMessage = message,
                )
            ),
          events = listOf(GenericUiEvent.ShowErrorSnackbar(message)),
        )
      }

    return EditEpisodeMatchResult(
      state =
        state.copy(
          match =
            match.copy(
              searchTerm = title,
              results = rawMatches.map { EditEpisodeMapper.mapMatchResult(it.episode) },
              isSearching = false,
              hasSearched = true,
              errorMessage = null,
            )
        ),
      events = emptyList(),
    )
  }

  suspend fun apply(state: EditEpisodeUiState, index: Int): EditEpisodeMatchResult {
    val match = state.match
    val result =
      match.results.getOrNull(index)
        ?: return EditEpisodeMatchResult(state = state.copy(isSaving = false), events = emptyList())

    val request =
      EditEpisodeMapper.buildMatchUpdateRequest(result)
        ?: return EditEpisodeMatchResult(
          state = state.copy(isSaving = false),
          events = listOf(GenericUiEvent.ShowPlainSnackbar(NO_UPDATES_NECESSARY)),
        )

    val updatedItem =
      updateEpisode(state.itemId, state.episodeId, request).getOrElse {
        return EditEpisodeMatchResult(
          state = state.copy(isSaving = false),
          events = listOf(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty())),
        )
      }

    updateCachedItem(updatedItem)

    val updatedDetails =
      (updatedItem.media as? Podcast)
        ?.episodes
        ?.find { it.id == state.episodeId }
        ?.let(EditEpisodeMapper::mapDetails)
        ?: EditEpisodeMapper.applyMatch(state.originalDetails, result)

    val syncedSearchTerm =
      if (
        match.searchTerm == state.originalDetails.title || match.searchTerm == state.details.title
      ) {
        updatedDetails.title
      } else {
        match.searchTerm
      }

    return EditEpisodeMatchResult(
      state =
        state.copy(
          currentTab = EditEpisodeTab.Details,
          details = updatedDetails,
          originalDetails = updatedDetails,
          match = match.copy(searchTerm = syncedSearchTerm),
          isSaving = false,
        ),
      events = listOf(GenericUiEvent.ShowSuccessSnackbar()),
    )
  }
}

private const val FAILED_TO_SEARCH_MATCHES = "Failed to search episode matches"
private const val NO_UPDATES_NECESSARY = "No updates necessary"
