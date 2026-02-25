package dev.halim.shelfdroid.core.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ChangeBehaviour
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerState.Hidden
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.service.StateHolder
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
  private val playerManager: Lazy<ExoPlayerManager>,
  private val playerRepository: PlayerRepository,
  private val stateHolder: StateHolder,
) : ViewModel() {

  private val _uiState = stateHolder.uiState

  val uiState: StateFlow<PlayerUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        viewModelScope.launch {
          when {
            _uiState.value.id != event.id -> {
              val advancedControl = _uiState.value.advancedControl
              val changeBehaviour = changeBehaviour(event)

              _uiState.update {
                playerRepository.playBook(
                  event.id,
                  event.isDownloaded,
                  advancedControl,
                  changeBehaviour,
                )
              }
              stateHolder.playContent()
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
              val advancedControl = _uiState.value.advancedControl
              val changeBehaviour = changeBehaviour(event)
              _uiState.update {
                playerRepository.playPodcast(
                  event.itemId,
                  event.episodeId,
                  event.isDownloaded,
                  advancedControl,
                  changeBehaviour,
                )
              }
              stateHolder.playContent()
            }
            _uiState.value.exoState == ExoState.Playing -> playerManager.get().pause()
            else -> playerManager.get().resume()
          }
        }
      }
      is PlayerEvent.ChangeChapter -> {
        _uiState.update { playerRepository.changeChapter(_uiState.value, event.target) }
        stateHolder.playContent()
      }
      PlayerEvent.SeekBackButton -> playerManager.get().seekBack()
      PlayerEvent.SeekForwardButton -> playerManager.get().seekForward()
      PlayerEvent.PlayPauseButton -> {
        if (_uiState.value.exoState == ExoState.Playing) playerManager.get().pause()
        else playerManager.get().resume()
      }
      is PlayerEvent.SeekTo -> {
        _uiState.update { playerRepository.seekTo(_uiState.value, event.target) }
        val positionMs = _uiState.value.currentTime.toLong() * 1000
        playerManager.get().seekTo(positionMs)
      }
      is PlayerEvent.ChangeSpeed -> {
        _uiState.update { playerRepository.changeSpeed(_uiState.value, event.speed) }
        playerManager.get().changeSpeed(event.speed)
      }
      is PlayerEvent.SleepTimer -> {
        if (event.duration != Duration.ZERO) {
          stateHolder.sleepTimer(event.duration)
        } else {
          stateHolder.clearTimer()
        }
      }
      PlayerEvent.NewBookmarkTime -> {
        val currentTimeInSeconds = playerManager.get().currentTime() / 1000
        _uiState.update { playerRepository.newBookmarkTime(_uiState.value, currentTimeInSeconds) }
      }

      is PlayerEvent.CreateBookmark -> {
        viewModelScope.launch {
          _uiState.update {
            playerRepository.createBookmark(_uiState.value, event.time, event.title)
          }
        }
      }
      is PlayerEvent.DeleteBookmark -> {
        viewModelScope.launch {
          _uiState.update { playerRepository.deleteBookmark(_uiState.value, event.bookmark) }
        }
      }
      is PlayerEvent.GoToBookmark -> {
        viewModelScope.launch {
          _uiState.update {
            val newUiState = playerRepository.goToBookmark(_uiState.value, event.time)
            newUiState
          }
          stateHolder.changeContent()
        }
      }
      is PlayerEvent.UpdateBookmark -> {
        viewModelScope.launch {
          _uiState.update {
            playerRepository.updateBookmark(_uiState.value, event.bookmark, event.title)
          }
        }
      }
      PlayerEvent.SkipPreviousButton -> {
        if (
          _uiState.value.playbackProgress.position > 3 ||
            _uiState.value.currentChapter?.isFirst() == true ||
            _uiState.value.currentChapter == null
        ) {
          playerManager.get().seekTo(0)
        } else {
          _uiState.update { playerRepository.previousNextChapter(_uiState.value, true) }
          stateHolder.playContent()
        }
      }
      PlayerEvent.SkipNextButton -> {
        _uiState.update { playerRepository.previousNextChapter(_uiState.value, false) }
        stateHolder.playContent()
      }
      PlayerEvent.Big -> _uiState.update { it.copy(state = PlayerState.Big) }
      PlayerEvent.Small -> _uiState.update { it.copy(state = PlayerState.Small) }
      PlayerEvent.TempHidden -> _uiState.update { it.copy(state = PlayerState.TempHidden) }
      PlayerEvent.Hidden -> _uiState.update { it.copy(state = Hidden()) }
      PlayerEvent.Logout -> logout()
    }
  }

  private fun changeBehaviour(event: PlayerEvent): ChangeBehaviour {
    val episodeIdBlank = _uiState.value.episodeId.isBlank()

    return when (event) {
      is PlayerEvent.PlayPodcast ->
        if (episodeIdBlank) ChangeBehaviour.Type else ChangeBehaviour.Episode

      is PlayerEvent.PlayBook -> if (episodeIdBlank) ChangeBehaviour.Book else ChangeBehaviour.Type

      else -> ChangeBehaviour.Type
    }
  }

  private fun logout() {
    _uiState.update { PlayerUiState() }
    playerManager.get().clearAndStop()
  }
}

sealed interface PlayerEvent {
  class PlayBook(val id: String, val isDownloaded: Boolean) : PlayerEvent

  class PlayPodcast(val itemId: String, val episodeId: String, val isDownloaded: Boolean) :
    PlayerEvent

  class ChangeChapter(val target: Int) : PlayerEvent

  class SeekTo(val target: Float) : PlayerEvent

  class ChangeSpeed(val speed: Float) : PlayerEvent

  class SleepTimer(val duration: Duration) : PlayerEvent

  class GoToBookmark(val time: Long) : PlayerEvent

  data object NewBookmarkTime : PlayerEvent

  class CreateBookmark(val time: Long, val title: String) : PlayerEvent

  class UpdateBookmark(val bookmark: PlayerBookmark, val title: String) : PlayerEvent

  class DeleteBookmark(val bookmark: PlayerBookmark) : PlayerEvent

  data object SeekBackButton : PlayerEvent

  data object SeekForwardButton : PlayerEvent

  data object PlayPauseButton : PlayerEvent

  data object SkipPreviousButton : PlayerEvent

  data object SkipNextButton : PlayerEvent

  data object Logout : PlayerEvent

  data object Big : PlayerEvent

  data object Small : PlayerEvent

  data object TempHidden : PlayerEvent

  data object Hidden : PlayerEvent
}
