package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.media.service.StateHolder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel
@Inject
constructor(
  private val repository: PodcastRepository,
  private val downloadRepo: DownloadRepo,
  stateHolder: StateHolder,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))
  private val addEpisodeState = MutableStateFlow<GenericState>(GenericState.Idle)

  val uiState: StateFlow<PodcastUiState> =
    combine(repository.item(id), stateHolder.uiState, addEpisodeState) {
        podcast,
        playerState,
        addState ->
        val updatedEpisodes =
          podcast.episodes.map { episode ->
            if (episode.episodeId == playerState.episodeId) {
              episode.copy(
                progress = playerState.playbackProgress.progress,
                isPlaying = playerState.exoState == ExoState.Playing,
              )
            } else episode
          }
        podcast.copy(episodes = updatedEpisodes, addEpisodeState = addState)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodcastUiState(),
      )

  fun onEvent(event: PodcastEvent) {
    when (event) {
      is PodcastEvent.ToggleIsFinished -> {
        viewModelScope.launch { repository.toggleIsFinished(id, event.episode) }
      }
      is PodcastEvent.Download -> {
        downloadRepo.download(event.downloadId, event.url, event.message)
      }
      is PodcastEvent.DeleteDownload -> {
        downloadRepo.delete(event.downloadId)
      }
      PodcastEvent.AddEpisode -> {
        addEpisodeState.update { GenericState.Loading }
        viewModelScope.launch(Dispatchers.IO) {
          addEpisodeState.update { repository.fetchEpisode() }
        }
      }
      PodcastEvent.ResetAddEpisodeState -> {
        addEpisodeState.update { GenericState.Idle }
      }
    }
  }
}

sealed class PodcastEvent {
  data class ToggleIsFinished(val episode: Episode) : PodcastEvent()

  data class Download(val downloadId: String, val url: String, val message: String) : PodcastEvent()

  data class DeleteDownload(val downloadId: String) : PodcastEvent()

  data object AddEpisode : PodcastEvent()

  data object ResetAddEpisodeState : PodcastEvent()
}
