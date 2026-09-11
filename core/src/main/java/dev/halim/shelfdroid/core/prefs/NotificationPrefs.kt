package dev.halim.shelfdroid.core.prefs

import dev.halim.shelfdroid.core.playback.DEFAULT_PLAYBACK_SPEED_CYCLE
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
data class NotificationPrefs(
  val sleepTimerMinutes: Int = 1,
  val firstAction: MediaNotificationAction = MediaNotificationAction.SleepTimer,
  val secondAction: MediaNotificationAction = MediaNotificationAction.NextChapter,
  val playbackSpeedCycle: List<Float> = DEFAULT_PLAYBACK_SPEED_CYCLE,
)
