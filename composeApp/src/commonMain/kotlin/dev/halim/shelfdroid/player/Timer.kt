package dev.halim.shelfdroid.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

sealed class TimerEvent {
    data object StartTimeListened : TimerEvent()
    data class StartSleepTimer(
        val duration: Duration,
        val updateTimeLeftCallback: (Duration) -> Unit,
        val timerFinishCallback: () -> Unit
    ) :
        TimerEvent()
}

sealed class TimerEventReturn {
    data object StopTimeListened : TimerEventReturn()
}

class Timer(private val main: CoroutineScope) {
    private var timeListened: Long = 0L
    private var sleepTime: Long = 0L
    private var timeLeft: Duration = Duration.INFINITE

    private var isPlaying = MutableStateFlow(false)
    private var timeCounterJob: Job? = null
    private var sleepTimerJob: Job? = null

    fun onEvent(event: TimerEvent) {
        when (event) {
            TimerEvent.StartTimeListened -> {
                startTimeListened()
            }

            is TimerEvent.StartSleepTimer -> {
                startSleepTimer(event.duration, event.updateTimeLeftCallback, event.timerFinishCallback)
            }
        }
    }

    fun onEventReturned(event: TimerEventReturn): Long {
        return when (event) {
            TimerEventReturn.StopTimeListened -> stopTimeListened()
        }
    }

    private fun startTimeListened() {
        isPlaying.update { true }
        timeCounterJob = main.launch {
            while (true) {
                timeListened += 1
                delay(1.toDuration(DurationUnit.SECONDS))
            }
        }
    }

    private fun stopTimeListened(): Long {
        isPlaying.update { false }
        val totalTime = timeListened
        timeListened = 0
        timeCounterJob?.cancel()
        return totalTime
    }

    private fun startSleepTimer(duration: Duration, sleepTimeLeft: (Duration) -> Unit, timerFinish: () -> Unit) {
        sleepTime = 0
        sleepTimerJob?.cancel()
        timeLeft = duration - sleepTime.toDuration(DurationUnit.SECONDS)
        sleepTimeLeft(timeLeft)

        sleepTimerJob = main.launch {
            while (sleepTime.toDuration(DurationUnit.SECONDS) < duration) {
                if (isPlaying.value) {
                    delay(1.toDuration(DurationUnit.SECONDS))
                    sleepTime += 1

                    timeLeft = duration - sleepTime.toDuration(DurationUnit.SECONDS)
                    sleepTimeLeft(timeLeft)

                    if (timeLeft <= Duration.ZERO) {
                        timerFinish()
                        break
                    }
                } else {
                    delay(500)
                }
            }
        }
    }
}