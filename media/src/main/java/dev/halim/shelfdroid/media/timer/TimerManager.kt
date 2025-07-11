package dev.halim.shelfdroid.media.timer

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TimerManager @Inject constructor(private val player: Lazy<ExoPlayer>) {

  var duration = MutableStateFlow(Duration.ZERO)
    private set

  private var timerFinished: () -> Unit = {}
  private var sleepJob: Job? = null
  private var isPlaying = false

  private val listener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        this@TimerManager.isPlaying = isPlaying

        if (isPlaying) {
          startSleepJob()
        } else {
          sleepJob?.cancel()
        }
      }
    }

  fun start(targetDuration: Duration, timerFinished: () -> Unit) {
    duration.value = targetDuration
    this.timerFinished = timerFinished
    player.get().removeListener(listener)
    player.get().addListener(listener)
    isPlaying = player.get().isPlaying
    if (isPlaying) {
      startSleepJob()
    }
  }

  private fun startSleepJob() {
    sleepJob?.cancel()
    sleepJob =
      CoroutineScope(Dispatchers.IO).launch {
        while (duration.value.inWholeSeconds > 0) {
          delay(1.seconds)
          Log.d("timer", duration.value.inWholeSeconds.toString())
          if (isPlaying) {
            duration.value -= 1.seconds
          }
        }
        CoroutineScope(Dispatchers.Main).launch {
          timerFinished()
          timerFinished = {}
          sleepJob?.cancel()
          player.get().removeListener(listener)
        }
      }
  }
}
