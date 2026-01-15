package dev.halim.shelfdroid.core.ui.screen.addepisode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeRepository
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddEpisodeViewModel
@Inject
constructor(private val repository: AddEpisodeRepository, savedStateHandle: SavedStateHandle) :
  ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))
  private val downloadEpisodeState = MutableStateFlow<GenericState>(GenericState.Idle)

  private val _uiState = MutableStateFlow(AddEpisodeUiState())

  val uiState: StateFlow<AddEpisodeUiState> =
    combine(_uiState, downloadEpisodeState) { uiState, downloadState ->
        uiState.copy(downloadEpisodeState = downloadState)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AddEpisodeUiState())

  init {
    viewModelScope.launch { _uiState.update { repository.item(id) } }
  }

  fun onEvent(event: AddEpisodeEvent) {
    when (event) {
      is AddEpisodeEvent.CheckEpisode -> {
        _uiState.update {
          val updatedEpisodes =
            it.episodes.map { episode ->
              if (episode.url == event.url) {
                val state =
                  if (event.isChecked) AddEpisodeDownloadState.ToBeDownloaded
                  else AddEpisodeDownloadState.NotDownloaded
                episode.copy(state = state)
              } else {
                episode
              }
            }
          it.copy(episodes = updatedEpisodes)
        }
      }

      AddEpisodeEvent.DownloadEpisodes -> {
        downloadEpisodeState.update { GenericState.Loading }
        viewModelScope.launch {
          val episodes =
            _uiState.value.episodes.filter { it.state == AddEpisodeDownloadState.ToBeDownloaded }
          downloadEpisodeState.update { repository.downloadEpisodes(id, episodes) }
        }
      }

      AddEpisodeEvent.ResetDownloadEpisodeState -> {
        downloadEpisodeState.update { GenericState.Idle }
      }
    }
  }
}

sealed interface AddEpisodeEvent {
  data class CheckEpisode(val url: String, val isChecked: Boolean) : AddEpisodeEvent

  data object DownloadEpisodes : AddEpisodeEvent

  data object ResetDownloadEpisodeState : AddEpisodeEvent
}
