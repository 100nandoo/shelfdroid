package dev.halim.shelfdroid.core.ui.components.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerState
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(private val playerRepository: PlayerRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(PlayerUiState())
  val uiState: StateFlow<PlayerUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        viewModelScope.launch {
          val success = playerRepository.playBook(event.id)
          if (success) {
            _uiState.update { playerRepository.book(event.id) }
          }
        }
      }
      is PlayerEvent.PlayPodcast -> {
        viewModelScope.launch {
          val success = playerRepository.playPodcast(event.itemId, event.episodeId)
          if (success) {
            _uiState.update { playerRepository.podcast(event.itemId, event.episodeId) }
          }
        }
      }
      PlayerEvent.Big -> _uiState.update { it.copy(state = PlayerState.Big) }
      PlayerEvent.Small -> _uiState.update { it.copy(state = PlayerState.Small) }
      PlayerEvent.TempHidden -> _uiState.update { it.copy(state = PlayerState.TempHidden) }
      PlayerEvent.Hidden -> _uiState.update { it.copy(state = PlayerState.Hidden()) }
    }
  }
}

sealed class PlayerEvent {
  class PlayBook(val id: String) : PlayerEvent()

  class PlayPodcast(val itemId: String, val episodeId: String) : PlayerEvent()

  data object Big : PlayerEvent()

  data object Small : PlayerEvent()

  data object TempHidden : PlayerEvent()

  data object Hidden : PlayerEvent()
}
