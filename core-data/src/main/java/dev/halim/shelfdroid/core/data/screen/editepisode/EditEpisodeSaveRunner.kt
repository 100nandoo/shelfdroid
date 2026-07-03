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
    val request = UpdatePodcastEpisodeRequest(title = state.title)
    val updatedItem =
      updateEpisode(state.itemId, state.episodeId, request).getOrElse {
        return EditEpisodeSaveResult(
          state = state.copy(isSaving = false),
          events = listOf(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty())),
        )
      }

    updateCachedItem(updatedItem)

    val updatedTitle =
      (updatedItem.media as? Podcast)
        ?.episodes
        ?.find { it.id == state.episodeId }
        ?.title
        ?: state.title

    return EditEpisodeSaveResult(
      state =
        state.copy(
          title = updatedTitle,
          persistedTitle = updatedTitle,
          isSaving = false,
        ),
      events = listOf(GenericUiEvent.ShowSuccessSnackbar()),
    )
  }
}
