package dev.halim.shelfdroid.media.exoplayer

import androidx.media3.common.util.Util.handlePlayPauseButtonAction
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import javax.inject.Inject

class ExoPlayerManager @Inject constructor(val player: Lazy<ExoPlayer>) {

  fun rawDuration(): Long {
    return player.get().duration
  }

  fun seekBack() {
    player.get().seekBack()
  }

  fun seekForward() {
    player.get().seekForward()
  }

  fun playPause() {
    handlePlayPauseButtonAction(player.get())
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
    player.get().seekTo(positionMs)
  }

  fun changeSpeed(speed: Float) {
    player.get().setPlaybackSpeed(speed)
  }
}
