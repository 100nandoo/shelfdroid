package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel
@Inject
constructor(private val podcastRepository: PodcastRepository, savedStateHandle: SavedStateHandle) :
  ViewModel() {
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
            updateEpisodeFinishedState(event.episode.id)
          }
        }
      }
    }
  }

  private fun initUiState() {
    viewModelScope.launch { _uiState.update { podcastRepository.item(id) } }
  }

  private fun updateEpisodeFinishedState(episodeId: String) {
    _uiState.update { currentState ->
      currentState.copy(
        episodes =
          currentState.episodes.map { episode ->
            if (episode.id == episodeId) {
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
}
