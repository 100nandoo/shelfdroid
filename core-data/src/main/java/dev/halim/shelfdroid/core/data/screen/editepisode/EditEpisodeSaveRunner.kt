package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.request.UpdatePodcastEpisodeRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericUiEvent

data class EditEpisodeSaveResult(
  val state: EditEpisodeUiState,
  val events: List<GenericUiEvent>,
)

class EditEpisodeSaveRunner(
  private val updateEpisode:
    suspend (itemId: String, episodeId: String, request: UpdatePodcastEpisodeRequest) -> Result<LibraryItem>,
  private val updateCachedItem: (LibraryItem) -> Unit = {},
) {
  suspend fun run(state: EditEpisodeUiState): EditEpisodeSaveResult {
    val request =
      EditEpisodeMapper.buildUpdateRequest(
        original = state.originalDetails,
        current = state.details,
      )
        ?: return EditEpisodeSaveResult(
          state = state.copy(isSaving = false),
          events = listOf(GenericUiEvent.ShowPlainSnackbar(NO_UPDATES_NECESSARY)),
        )

    val updatedItem =
      updateEpisode(state.itemId, state.episodeId, request).getOrElse {
        return EditEpisodeSaveResult(
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
        ?: state.details

    return EditEpisodeSaveResult(
      state =
        state.copy(
          details = updatedDetails,
          originalDetails = updatedDetails,
          isSaving = false,
        ),
      events = listOf(GenericUiEvent.ShowSuccessSnackbar()),
    )
  }
}

private const val NO_UPDATES_NECESSARY = "No updates necessary"
