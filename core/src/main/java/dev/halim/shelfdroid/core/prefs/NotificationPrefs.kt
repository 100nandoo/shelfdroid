package dev.halim.shelfdroid.core.prefs

import dev.halim.shelfdroid.core.playback.DEFAULT_PLAYBACK_SPEED_CYCLE
import dev.halim.shelfdroid.core.playback.DEFAULT_SLEEP_TIMER_CYCLE
import kotlinx.serialization.Serializable

@Serializable
enum class MediaNotificationAction {
  SleepTimer,
  NextChapter,
  PreviousChapter,
  PlaybackSpeed,
  None,
}

@Serializable
enum class SleepTimerNotificationMode {
  Toggle,
  Cyclical,
}

@Serializable
data class NotificationPrefs(
  val sleepTimerMinutes: Int = 1,
  val firstAction: MediaNotificationAction = MediaNotificationAction.SleepTimer,
  val secondAction: MediaNotificationAction = MediaNotificationAction.NextChapter,
  val playbackSpeedCycle: List<Float> = DEFAULT_PLAYBACK_SPEED_CYCLE,
  val sleepTimerMode: SleepTimerNotificationMode = SleepTimerNotificationMode.Toggle,
  val sleepTimerCycle: List<Int> = DEFAULT_SLEEP_TIMER_CYCLE,
)
