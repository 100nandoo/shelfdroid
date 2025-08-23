package dev.halim.shelfdroid.media.exoplayer

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

sealed class PlayerEvent {
  object Pause : PlayerEvent()

  object Resume : PlayerEvent()
}

@Singleton
class ExoPlayerManager @Inject constructor(val player: Lazy<ExoPlayer>) {

  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val _events = MutableSharedFlow<PlayerEvent>()
  val events: SharedFlow<PlayerEvent> = _events

  fun isPlaying() = player.get().isPlaying

  suspend fun isPlayingSafe(): Boolean =
    withContext(Dispatchers.Main) {
      val result = player.get().isPlaying
      return@withContext result
    }

  fun rawDuration(): Long {
    return player.get().duration
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
    emit(PlayerEvent.Pause)
  }

  fun resume() {
    player.get().play()
    emit(PlayerEvent.Resume)
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

  private fun emit(event: PlayerEvent) {
    syncScope.launch { _events.emit(event) }
  }
}
