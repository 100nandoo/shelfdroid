package dev.halim.shelfdroid.core.ui.player

import dagger.Lazy
import dev.halim.shelfdroid.core.ChangeBehaviour
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerState.Hidden
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.player.PreparedPlayback
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.media.di.MediaControllerManager
import dev.halim.shelfdroid.media.playback.PlayerStore
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerController
@Inject
constructor(
  private val mediaControl: Lazy<MediaControllerManager>,
  private val playerRepository: PlayerRepository,
  private val playerStore: PlayerStore,
  @Named("io") private val scope: CoroutineScope,
  @Named("main") private val mainScope: CoroutineScope,
) {
  private val uiState
    get() = playerStore.uiState
  private val playbackRequestId = AtomicLong()

  fun hasCurrentPlayback(): Boolean = uiState.value.id.isNotBlank()

  fun onEvent(event: PlayerEvent) {
    when (event) {
      is PlayerEvent.PlayBook -> {
        val requestId = playbackRequestId.incrementAndGet()
        mainScope.launch {
          when {
            uiState.value.id != event.id -> {
              val advancedControl = uiState.value.advancedControl
              val changeBehaviour = changeBehaviour(event)

              val playback =
                playerRepository.prepareBookPlayback(event.id, advancedControl, changeBehaviour)
              completePlayback(requestId, playback)
            }
            else -> mediaControl.get().playPause()
          }
        }
      }
      is PlayerEvent.PlayPodcast -> {
        val requestId = playbackRequestId.incrementAndGet()
        mainScope.launch {
          when {
            uiState.value.episodeId != event.episodeId -> {
              val advancedControl = uiState.value.advancedControl
              val changeBehaviour = changeBehaviour(event)
              val playback =
                playerRepository.preparePodcastPlayback(
                  event.itemId,
                  event.episodeId,
                  advancedControl,
                  changeBehaviour,
                )
              completePlayback(requestId, playback)
            }
            else -> mediaControl.get().playPause()
          }
        }
      }
      is PlayerEvent.ChangeChapter -> {
        uiState.update { playerRepository.changeChapter(it, event.target) }
        if (uiState.value.state !is Hidden) playerStore.playContent()
      }
      PlayerEvent.SeekBackButton -> mediaControl.get().seekBack()
      PlayerEvent.SeekForwardButton -> mediaControl.get().seekForward()
      PlayerEvent.PlayPauseButton -> mediaControl.get().playPause()
      is PlayerEvent.SeekTo -> {
        uiState.update { playerRepository.seekTo(it, event.target) }
        val positionMs = uiState.value.currentTime.toLong() * 1000
        mediaControl.get().seekTo(positionMs)
      }
      is PlayerEvent.ChangeSpeed -> {
        uiState.update { playerRepository.changeSpeed(it, event.speed) }
        mediaControl.get().changeSpeed(event.speed)
      }
      is PlayerEvent.SleepTimer -> {
        if (event.duration != Duration.ZERO) {
          playerStore.sleepTimer(event.duration)
        } else {
          playerStore.clearTimer()
        }
      }
      PlayerEvent.NewBookmarkTime -> {
        val currentTimeInSeconds = mediaControl.get().currentPosition() / 1000
        uiState.update { playerRepository.newBookmarkTime(it, currentTimeInSeconds) }
      }

      is PlayerEvent.CreateBookmark -> {
        val itemId = uiState.value.id
        scope.launch {
          val bookmark = playerRepository.createBookmark(itemId, event.time, event.title)
          if (bookmark != null) {
            uiState.update { current ->
              if (current.id == itemId) {
                current.copy(playerBookmarks = current.playerBookmarks + bookmark)
              } else current
            }
          }
        }
      }
      is PlayerEvent.DeleteBookmark -> {
        val itemId = uiState.value.id
        scope.launch {
          if (playerRepository.deleteBookmark(itemId, event.bookmark)) {
            uiState.update { current ->
              if (current.id == itemId) {
                current.copy(playerBookmarks = current.playerBookmarks - event.bookmark)
              } else current
            }
          }
        }
      }
      is PlayerEvent.GoToBookmark -> {
        uiState.update { playerRepository.goToBookmark(it, event.time) }
        playerStore.changeContent()
      }
      is PlayerEvent.UpdateBookmark -> {
        val itemId = uiState.value.id
        scope.launch {
          if (playerRepository.updateBookmark(itemId, event.bookmark, event.title)) {
            uiState.update { current ->
              if (current.id == itemId) {
                current.copy(
                  playerBookmarks =
                    current.playerBookmarks.map {
                      if (it.time == event.bookmark.time) it.copy(title = event.title) else it
                    }
                )
              } else current
            }
          }
        }
      }
      PlayerEvent.SkipPreviousButton -> {
        if (
          uiState.value.playbackProgress.position > 3 ||
            uiState.value.currentChapter?.isFirst() == true ||
            uiState.value.currentChapter == null
        ) {
          mediaControl.get().seekTo(0)
        } else {
          uiState.update { playerRepository.previousNextChapter(it, true) }
          if (uiState.value.state !is Hidden) playerStore.playContent()
        }
      }
      PlayerEvent.SkipNextButton -> {
        uiState.update { playerRepository.previousNextChapter(it, false) }
        if (uiState.value.state !is Hidden) playerStore.playContent()
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

  private fun completePlayback(requestId: Long, result: Result<PreparedPlayback>) {
    if (requestId != playbackRequestId.get()) return
    result.fold(
      onSuccess = { playback ->
        playerRepository.activatePlayback(playback)
        uiState.value = playback.uiState
        playerStore.playContent()
      },
      onFailure = { error ->
        playerStore.emptyState()
        uiState.value = PlayerUiState(state = Hidden(error))
        mediaControl.get().clearAndStop()
      },
    )
  }

  private fun logout() {
    playbackRequestId.incrementAndGet()
    uiState.update { playerStore.emptyState() }
    mediaControl.get().clearAndStop()
  }
}

sealed interface PlayerEvent {
  class PlayBook(val id: String) : PlayerEvent

  class PlayPodcast(val itemId: String, val episodeId: String) : PlayerEvent

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
