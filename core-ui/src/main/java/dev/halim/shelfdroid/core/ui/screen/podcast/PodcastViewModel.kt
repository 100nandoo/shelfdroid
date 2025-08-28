package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.download.DownloadTracker
import dev.halim.shelfdroid.media.service.StateHolder
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel
@Inject
constructor(
  private val podcastRepository: PodcastRepository,
  private val downloadTracker: DownloadTracker,
  stateHolder: StateHolder,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))

  val uiState: StateFlow<PodcastUiState> =
    combine(podcastRepository.item(id), stateHolder.uiState) { podcast, state ->
        val updatedEpisodes =
          podcast.episodes.map { episode ->
            if (episode.episodeId == state.episodeId) {
              episode.copy(
                progress = state.playbackProgress.progress,
                isPlaying = state.exoState == ExoState.Playing,
              )
            } else episode
          }
        podcast.copy(episodes = updatedEpisodes)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PodcastUiState(),
      )

  fun onEvent(event: PodcastEvent) {
    when (event) {
      is PodcastEvent.ToggleIsFinished -> {
        viewModelScope.launch { podcastRepository.toggleIsFinished(id, event.episode) }
      }
      is PodcastEvent.Download -> {
        downloadTracker.download(event.downloadId, event.url, event.message)
      }
      is PodcastEvent.DeleteDownload -> {
        downloadTracker.delete(event.downloadId)
      }
    }
  }
}

sealed class PodcastEvent {
  data class ToggleIsFinished(val episode: Episode) : PodcastEvent()

  data class Download(val downloadId: String, val url: String, val message: String) :
    PodcastEvent()

  data class DeleteDownload(val downloadId: String) : PodcastEvent()
}
