package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.media.download.DownloadTracker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel
@Inject
constructor(
  private val podcastRepository: PodcastRepository,
  private val downloadRepo: DownloadRepo,
  private val downloadTracker: DownloadTracker,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))

  private val _uiState = MutableStateFlow(PodcastUiState())
  val uiState: StateFlow<PodcastUiState> = _uiState.asStateFlow()

  init {
    initUiState()
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

  private fun initUiState() {
    viewModelScope.launch {
      _uiState.update { podcastRepository.item(id) }
      downloadRepo.downloads.collect { downloads ->
        _uiState.update { podcastRepository.updateDownloads(it, downloads) }
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
