package dev.halim.shelfdroid.media.misc

import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.exoplayer.PlayerEvent
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerManager @Inject constructor(private val exoPlayerManager: ExoPlayerManager) {

  var duration = MutableStateFlow(Duration.ZERO)
    private set

  private var timerFinished: () -> Unit = {}
  private var sleepJob: Job? = null
  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  init {
    syncScope.launch {
      exoPlayerManager.events.collect { event ->
        when (event) {
          PlayerEvent.Pause -> {
            sleepJob?.cancel()
          }
          PlayerEvent.Resume -> {
            startSleepJob()
          }
        }
      }
    }
  }

  fun start(targetDuration: Duration, timerFinished: () -> Unit) {
    duration.value = targetDuration
    this.timerFinished = timerFinished
    if (exoPlayerManager.isPlaying()) {
      startSleepJob()
    }
  }

  fun clear() {
    sleepJob?.cancel()
    duration.value = Duration.ZERO
    timerFinished = {}
  }

  private fun startSleepJob() {
    sleepJob?.cancel()
    sleepJob =
      syncScope.launch {
        while (duration.value.inWholeSeconds > 0) {
          delay(1.seconds)
          if (exoPlayerManager.isPlayingSafe()) {
            duration.value -= 1.seconds
          }
        }

        withContext(Dispatchers.Main) {
          timerFinished()
          timerFinished = {}
          sleepJob?.cancel()
        }
      }
  }
}
