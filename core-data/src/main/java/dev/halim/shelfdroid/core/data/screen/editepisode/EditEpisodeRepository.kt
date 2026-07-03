package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

class EditEpisodeRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepo,
) {
  private val saveRunner =
    EditEpisodeSaveRunner(
      updateEpisode = { itemId, episodeId, request ->
        api.updatePodcastEpisode(itemId, episodeId, request)
      },
      updateCachedItem = libraryItemRepo::updateItem,
    )

  suspend fun item(itemId: String, episodeId: String): EditEpisodeUiState {
    val podcastTitle = libraryItemRepo.byId(itemId)?.title.orEmpty()
    val episode =
      api.podcastEpisode(itemId, episodeId).getOrElse {
        return EditEpisodeUiState(
          state = GenericState.Failure(it.message ?: "Failed to load episode"),
          itemId = itemId,
          episodeId = episodeId,
          podcastTitle = podcastTitle,
        )
      }

    return EditEpisodeMapper.mapState(itemId, episodeId, podcastTitle, episode)
  }

  suspend fun save(
    state: EditEpisodeUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditEpisodeUiState {
    val result = saveRunner.run(state)
    result.events.forEach { events.emit(it) }
    return result.state
  }
}
