package dev.halim.shelfdroid.media.exoplayer

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PlayerEvent {
  object Pause : PlayerEvent

  object Resume : PlayerEvent
}

@Singleton
class ExoPlayerManager @Inject constructor() {

  @Inject lateinit var player: Lazy<ExoPlayer>

  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val _events = MutableSharedFlow<PlayerEvent>()
  val events: SharedFlow<PlayerEvent> = _events
  @Volatile private var isItemChanged = false

  private val listener =
    object : Player.Listener {
      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        isItemChanged = true
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        if (isPlaying) {
          emit(PlayerEvent.Resume)
          isItemChanged = false
        } else {
          emit(PlayerEvent.Pause)
        }
      }
    }

  fun isPlaying() = player.get().isPlaying

  fun isItemChanged() = isItemChanged

  suspend fun isPlayingSafe(): Boolean =
    withContext(Dispatchers.Main) {
      val result = player.get().isPlaying
      return@withContext result
    }

  suspend fun currentPosition(): Long {
    return withContext(Dispatchers.Main) { player.get().currentPosition }
  }

  fun seekBack() {
    player.get().seekBack()
  }

  fun seekForward() {
    player.get().seekForward()
  }

  fun pause() {
    player.get().pause()
  }

  fun resume() {
    player.get().play()
  }

  fun clearAndStop() {
    player.get().apply {
      stop()
      clearMediaItems()
    }
  }

  fun seekTo(positionMs: Long) {
    player.get().apply {
      if (mediaItemCount > 1) {
        val window = Timeline.Window()
        var sum = 0L
        for (i in 0 until currentTimeline.windowCount) {
          currentTimeline.getWindow(i, window)
          val windowDuration = window.durationMs
          if (positionMs < sum + windowDuration) {
            seekTo(i, positionMs - sum)
            return
          }
          sum += windowDuration
        }
        currentTimeline.getWindow(currentTimeline.windowCount - 1, window)
        seekTo(currentTimeline.windowCount - 1, window.durationMs)
      } else {
        seekTo(positionMs)
      }
    }
  }

  fun changeSpeed(speed: Float) {
    player.get().setPlaybackSpeed(speed)
  }

  fun currentTime(): Long {
    return player.get().currentPosition
  }

  fun addListener(listener: Player.Listener) {
    player.get().addListener(listener)
  }

  fun addDefaultListener() {
    player.get().addListener(listener)
  }

  fun currentMediaItem(): MediaItem? {
    return player.get().currentMediaItem
  }

  private fun emit(event: PlayerEvent) {
    syncScope.launch { _events.emit(event) }
  }
}
