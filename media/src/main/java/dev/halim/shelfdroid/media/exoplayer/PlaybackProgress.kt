package dev.halim.shelfdroid.media.exoplayer

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import dev.halim.shelfdroid.core.RawPlaybackProgress
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
        var position = currentPosition
        val isNotFirstMediaItem = mediaItemCount > 1 && currentMediaItemIndex != 0
        if (isNotFirstMediaItem) {
          val totalPreviousDurations = sumPreviousDurations(currentTimeline, currentMediaItemIndex)
          position = totalPreviousDurations + currentPosition
        }
        trySend(RawPlaybackProgress(positionMs = position, bufferedPosition = bufferedPosition))
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

private fun sumPreviousDurations(timeline: Timeline, upToIndex: Int): Long {
  var sum: Long = 0
  val window = Timeline.Window()
  for (i in 0 until upToIndex) {
    timeline.getWindow(i, window)
    sum += window.durationMs
  }
  return sum
}
