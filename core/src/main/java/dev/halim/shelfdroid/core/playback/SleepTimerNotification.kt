package dev.halim.shelfdroid.core.playback

import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun NotificationPrefs.nextSleepTimerDuration(
  isSleepTimerActive: Boolean,
  sleepTimerDuration: Duration,
): Duration {
  return when (sleepTimerMode) {
    SleepTimerNotificationMode.Toggle ->
      if (isSleepTimerActive) Duration.ZERO else sleepTimerMinutes.minutes
    SleepTimerNotificationMode.Cyclical -> {
      val cycle = normalizeSleepTimerCycle(sleepTimerCycle).map { it.minutes }
      if (!isSleepTimerActive) {
        cycle.first()
      } else {
        val index = cycle.indexOf(sleepTimerDuration)
        if (index >= 0) cycle.getOrNull(index + 1) ?: Duration.ZERO else Duration.ZERO
      }
    }
  }
}
