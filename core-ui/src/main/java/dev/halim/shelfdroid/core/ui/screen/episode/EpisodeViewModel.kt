package dev.halim.shelfdroid.core.ui.screen.episode

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.screen.episode.EpisodeRepository
import dev.halim.shelfdroid.core.data.screen.episode.EpisodeUiState
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.media.download.DownloadTracker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: EpisodeRepository,
  private val downloadRepo: DownloadRepo,
  private val downloadTracker: DownloadTracker,
) : ViewModel() {

  val itemId: String = checkNotNull(savedStateHandle.get<String>("itemId"))
  val episodeId: String = checkNotNull(savedStateHandle.get<String>("episodeId"))

  private val _uiState = MutableStateFlow(EpisodeUiState())
  val uiState: StateFlow<EpisodeUiState> =
    _uiState
      .onStart { initUiState() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EpisodeUiState())

  fun onEvent(event: EpisodeEvent) {
    when (event) {
      is EpisodeEvent.DownloadEvent -> {
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

  @SuppressLint("UnsafeOptInUsageError")
  private fun initUiState() {
    viewModelScope.launch {
      _uiState.update { repository.item(itemId, episodeId) }

      downloadRepo.downloads
        .mapNotNull { downloads -> downloads.find { it.request.id == _uiState.value.download.id } }
        .collect { download -> _uiState.update { repository.updateDownloads(it, download) } }
    }
  }
}

sealed class EpisodeEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : EpisodeEvent()
}
