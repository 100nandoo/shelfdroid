package dev.halim.shelfdroid.media.exoplayer

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.halim.shelfdroid.core.data.screen.player.RawPlaybackProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

fun ExoPlayer.playbackProgressFlow(): Flow<RawPlaybackProgress> = callbackFlow {
  var job: Job? = null

  fun startPlayerTracking() {
    job?.cancel()
    job = launch {
      while (true) {
        trySend(
          RawPlaybackProgress(
            positionMs = currentPosition,
            durationMs = duration,
            bufferedPosition = bufferedPosition,
          )
        )
        delay(500)
      }
    }
  }

  val listener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
          startPlayerTracking()
        } else {
          job?.cancel()
        }
      }
    }

  addListener(listener)

  if (isPlaying) {
    startPlayerTracking()
  }

  awaitClose {
    removeListener(listener)
    job?.cancel()
  }
}
