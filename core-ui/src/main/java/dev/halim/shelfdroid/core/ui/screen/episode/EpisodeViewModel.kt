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
import dev.halim.shelfdroid.download.DownloadTracker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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
    val download = _uiState.value.download
    when (event) {
      is EpisodeEvent.DownloadEvent -> {
        when (event.downloadEvent) {
          is CommonDownloadEvent.Download -> {
            downloadTracker.download(download.id, download.url, download.title)
          }
          is CommonDownloadEvent.DeleteDownload -> {
            downloadTracker.delete(download.id)
          }
        }
      }
    }
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun initUiState() {
    viewModelScope.launch {
      repository.item(itemId, episodeId).collect { episodeUiState ->
        _uiState.value = episodeUiState
      }
    }
  }
}

sealed class EpisodeEvent {
  data class DownloadEvent(val downloadEvent: CommonDownloadEvent) : EpisodeEvent()
}
