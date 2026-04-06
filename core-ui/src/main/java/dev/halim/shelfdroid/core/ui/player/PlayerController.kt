package dev.halim.shelfdroid.core.ui.player

import dagger.Lazy
import dev.halim.shelfdroid.core.ChangeBehaviour
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerState.Hidden
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.media.di.MediaControllerManager
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.service.PlayerStore
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerController
@Inject
constructor(
  private val playerManager: Lazy<ExoPlayerManager>,
  private val mediaControl: Lazy<MediaControllerManager>,
  private val playerRepository: PlayerRepository,
  private val playerStore: PlayerStore,
  @Named("io") private val scope: CoroutineScope,
  @Named("main") private val mainScope: CoroutineScope,
) {
  private val uiState
    get() = playerStore.uiState

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        mainScope.launch {
          when {
            uiState.value.id != event.id -> {
              val advancedControl = uiState.value.advancedControl
              val changeBehaviour = changeBehaviour(event)

              uiState.update {
                playerRepository.playBook(
                  event.id,
                  event.isDownloaded,
                  advancedControl,
                  changeBehaviour,
                )
              }
              playerStore.playContent()
            }
            uiState.value.exoState == ExoState.Playing -> playerManager.get().pause()
            else -> playerManager.get().resume()
          }
        }
      }
      is PlayerEvent.PlayPodcast -> {
        mainScope.launch {
          when {
            uiState.value.episodeId != event.episodeId -> {
              val advancedControl = uiState.value.advancedControl
              val changeBehaviour = changeBehaviour(event)
              uiState.update {
                playerRepository.playPodcast(
                  event.itemId,
                  event.episodeId,
                  event.isDownloaded,
                  advancedControl,
                  changeBehaviour,
                )
              }
              playerStore.playContent()
            }
            uiState.value.exoState == ExoState.Playing -> playerManager.get().pause()
            else -> playerManager.get().resume()
          }
        }
      }
      is PlayerEvent.ChangeChapter -> {
        uiState.update { playerRepository.changeChapter(uiState.value, event.target) }
        playerStore.playContent()
      }
      PlayerEvent.SeekBackButton -> mediaControl.get().seekBack()
      PlayerEvent.SeekForwardButton -> mediaControl.get().seekForward()
      PlayerEvent.PlayPauseButton -> mediaControl.get().playPause()
      is PlayerEvent.SeekTo -> {
        uiState.update { playerRepository.seekTo(uiState.value, event.target) }
        val positionMs = uiState.value.currentTime.toLong() * 1000
        playerManager.get().seekTo(positionMs)
      }
      is PlayerEvent.ChangeSpeed -> {
        uiState.update { playerRepository.changeSpeed(uiState.value, event.speed) }
        playerManager.get().changeSpeed(event.speed)
      }
      is PlayerEvent.SleepTimer -> {
        if (event.duration != Duration.ZERO) {
          playerStore.sleepTimer(event.duration)
        } else {
          playerStore.clearTimer()
        }
      }
      PlayerEvent.NewBookmarkTime -> {
        val currentTimeInSeconds = playerManager.get().currentTime() / 1000
        uiState.update { playerRepository.newBookmarkTime(uiState.value, currentTimeInSeconds) }
      }

      is PlayerEvent.CreateBookmark -> {
        scope.launch {
          uiState.update { playerRepository.createBookmark(uiState.value, event.time, event.title) }
        }
      }
      is PlayerEvent.DeleteBookmark -> {
        scope.launch {
          uiState.update { playerRepository.deleteBookmark(uiState.value, event.bookmark) }
        }
      }
      is PlayerEvent.GoToBookmark -> {
        scope.launch {
          uiState.update {
            val newUiState = playerRepository.goToBookmark(uiState.value, event.time)
            newUiState
          }
          playerStore.changeContent()
        }
      }
      is PlayerEvent.UpdateBookmark -> {
        scope.launch {
          uiState.update {
            playerRepository.updateBookmark(uiState.value, event.bookmark, event.title)
          }
        }
      }
      PlayerEvent.SkipPreviousButton -> {
        if (
          uiState.value.playbackProgress.position > 3 ||
            uiState.value.currentChapter?.isFirst() == true ||
            uiState.value.currentChapter == null
        ) {
          playerManager.get().seekTo(0)
        } else {
          uiState.update { playerRepository.previousNextChapter(uiState.value, true) }
          playerStore.playContent()
        }
      }
      PlayerEvent.SkipNextButton -> {
        uiState.update { playerRepository.previousNextChapter(uiState.value, false) }
        playerStore.playContent()
      }
      PlayerEvent.Big -> uiState.update { it.copy(state = PlayerState.Big) }
      PlayerEvent.Small -> uiState.update { it.copy(state = PlayerState.Small) }
      PlayerEvent.TempHidden -> uiState.update { it.copy(state = PlayerState.TempHidden) }
      PlayerEvent.Hidden -> uiState.update { it.copy(state = Hidden()) }
      PlayerEvent.Logout -> logout()
    }
  }

  private fun changeBehaviour(event: PlayerEvent): ChangeBehaviour {
    val episodeIdBlank = uiState.value.episodeId.isBlank()

    return when (event) {
      is PlayerEvent.PlayPodcast ->
        if (episodeIdBlank) ChangeBehaviour.Type else ChangeBehaviour.Episode

      is PlayerEvent.PlayBook -> if (episodeIdBlank) ChangeBehaviour.Book else ChangeBehaviour.Type

      else -> ChangeBehaviour.Type
    }
  }

  private fun logout() {
    uiState.update { playerStore.emptyState() }
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
