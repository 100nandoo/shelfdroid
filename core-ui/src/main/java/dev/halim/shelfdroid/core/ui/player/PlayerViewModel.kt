package dev.halim.shelfdroid.core.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.player.ExoState
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerState
import dev.halim.shelfdroid.core.data.screen.player.PlayerState.Hidden
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.service.ServiceUiStateHolder
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
  private val playerManager: Lazy<ExoPlayerManager>,
  private val playerRepository: PlayerRepository,
  private val serviceUiStateHolder: ServiceUiStateHolder,
) : ViewModel() {

  private val _uiState = serviceUiStateHolder.uiState

  val uiState: StateFlow<PlayerUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        viewModelScope.launch {
          when {
            _uiState.value.id != event.id -> {
              _uiState.update { playerRepository.playBook(event.id) }
              serviceUiStateHolder.playContent()
            }
            _uiState.value.exoState == ExoState.Playing -> playerManager.get().pause()
            else -> playerManager.get().resume()
          }
        }
      }
      is PlayerEvent.PlayPodcast -> {
        viewModelScope.launch {
          when {
            _uiState.value.episodeId != event.episodeId -> {
              _uiState.update { playerRepository.playPodcast(event.itemId, event.episodeId) }
              serviceUiStateHolder.playContent()
            }
            _uiState.value.exoState == ExoState.Playing -> playerManager.get().pause()
            else -> playerManager.get().resume()
          }
        }
      }
      is PlayerEvent.ChangeChapter -> {
        _uiState.update { playerRepository.changeChapter(_uiState.value, event.target) }
        serviceUiStateHolder.playContent()
      }
      PlayerEvent.SeekBack -> playerManager.get().seekBack()
      PlayerEvent.SeekForward -> playerManager.get().seekForward()
      PlayerEvent.PlayPause -> playerManager.get().playPause()
      is PlayerEvent.SeekTo -> {
        val durationMs = playerManager.get().rawDuration()
        _uiState.update { playerRepository.seekTo(_uiState.value, event.target, durationMs) }
        val positionMs = _uiState.value.currentTime.toLong() * 1000
        playerManager.get().seekTo(positionMs)
      }
      is PlayerEvent.ChangeSpeed -> {
        _uiState.update { playerRepository.changeSpeed(_uiState.value, event.speed) }
        playerManager.get().changeSpeed(event.speed)
      }
      is PlayerEvent.SleepTimer -> {
        serviceUiStateHolder.sleepTimer(event.duration)
      }

      PlayerEvent.PreviousChapter -> {
        _uiState.update { playerRepository.previousNextChapter(_uiState.value, true) }
        serviceUiStateHolder.playContent()
      }
      PlayerEvent.NextChapter -> {
        _uiState.update { playerRepository.previousNextChapter(_uiState.value, false) }
        serviceUiStateHolder.playContent()
      }
      PlayerEvent.Big -> _uiState.update { it.copy(state = PlayerState.Big) }
      PlayerEvent.Small -> _uiState.update { it.copy(state = PlayerState.Small) }
      PlayerEvent.TempHidden -> _uiState.update { it.copy(state = PlayerState.TempHidden) }
      PlayerEvent.Hidden -> _uiState.update { it.copy(state = Hidden()) }
      PlayerEvent.Logout -> logout()
    }
  }

  private fun logout() {
    _uiState.update { PlayerUiState() }
    playerManager.get().clearAndStop()
  }
}

sealed class PlayerEvent {
  class PlayBook(val id: String) : PlayerEvent()

  class PlayPodcast(val itemId: String, val episodeId: String) : PlayerEvent()

  class ChangeChapter(val target: Int) : PlayerEvent()

  class SeekTo(val target: Float) : PlayerEvent()

  class ChangeSpeed(val speed: Float) : PlayerEvent()

  class SleepTimer(val duration: Duration) : PlayerEvent()

  data object SeekBack : PlayerEvent()

  data object SeekForward : PlayerEvent()

  data object PlayPause : PlayerEvent()

  data object PreviousChapter : PlayerEvent()

  data object NextChapter : PlayerEvent()

  data object Logout : PlayerEvent()

  data object Big : PlayerEvent()

  data object Small : PlayerEvent()

  data object TempHidden : PlayerEvent()

  data object Hidden : PlayerEvent()
}
