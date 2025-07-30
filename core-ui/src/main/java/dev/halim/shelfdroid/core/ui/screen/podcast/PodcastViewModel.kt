package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.media.download.DownloadTracker
import dev.halim.shelfdroid.media.service.StateHolder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel
@Inject
constructor(
  private val podcastRepository: PodcastRepository,
  private val downloadTracker: DownloadTracker,
  private val stateHolder: StateHolder,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))

  private val _uiState = MutableStateFlow(PodcastUiState())
  val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(podcastRepository.item(id), stateHolder.uiState) { podcast, state ->
          val updatedEpisodes =
            podcast.episodes.map { episode ->
              if (episode.episodeId == state.episodeId) {
                episode.copy(
                  progress = state.playbackProgress.progress,
                  isPlaying = state.exoState == ExoState.Playing,
                )
              } else {
                episode
              }
            }
          podcast.copy(episodes = updatedEpisodes)
        }
        .collect { updatedPodcast -> _uiState.value = updatedPodcast }
    }
  }

  fun onEvent(event: PodcastEvent) {
    when (event) {
      is PodcastEvent.ToggleIsFinished -> {
        viewModelScope.launch {
          val isSuccess = podcastRepository.toggleIsFinished(id, event.episode)
          if (isSuccess) {
            updateEpisodeFinishedState(event.episode.episodeId)
          }
        }
      }
      is PodcastEvent.DownloadEvent -> {
        when (event.downloadEvent) {
          is CommonDownloadEvent.Download -> {
            downloadTracker.download(event.downloadEvent.downloadId, event.downloadEvent.url)
          }
          is CommonDownloadEvent.DeleteDownload -> {
            downloadTracker.delete(event.downloadEvent.downloadId)
          }
        }
      }
    }
  }

  private fun updateEpisodeFinishedState(episodeId: String) {
    _uiState.update { currentState ->
      currentState.copy(
        episodes =
          currentState.episodes.map { episode ->
            if (episode.episodeId == episodeId) {
              episode.copy(isFinished = !episode.isFinished)
            } else {
              episode
            }
          }
      )
    }
  }
}

sealed class PodcastEvent {
  data class ToggleIsFinished(val episode: Episode) : PodcastEvent()

  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : PodcastEvent()
}
