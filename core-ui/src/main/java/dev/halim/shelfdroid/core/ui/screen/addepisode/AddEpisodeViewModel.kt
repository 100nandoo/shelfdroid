package dev.halim.shelfdroid.core.ui.screen.addepisode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeRepository
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddEpisodeViewModel
@Inject
constructor(repository: AddEpisodeRepository, savedStateHandle: SavedStateHandle) : ViewModel() {
  val id: String = checkNotNull(savedStateHandle.get<String>("id"))

  private val _uiState = MutableStateFlow(AddEpisodeUiState())

  val uiState: StateFlow<AddEpisodeUiState> = _uiState

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
                episode.copy(isDownloaded = event.isChecked)
              } else {
                episode
              }
            }
          it.copy(episodes = updatedEpisodes)
        }
      }
    }
  }
}

sealed class AddEpisodeEvent {
  data class CheckEpisode(val url: String, val isChecked: Boolean) : AddEpisodeEvent()
}
