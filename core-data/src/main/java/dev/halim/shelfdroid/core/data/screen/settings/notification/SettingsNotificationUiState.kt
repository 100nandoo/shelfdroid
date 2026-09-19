package dev.halim.shelfdroid.core.data.screen.settings.notification

import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationTapDestination
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import dev.halim.shelfdroid.core.prefs.PlaybackSpeedNotificationMode

data class SettingsNotificationUiState(
  val tapDestination: MediaNotificationTapDestination = NotificationPrefs().tapDestination,
  val sleepTimerMinutes: Int = NotificationPrefs().sleepTimerMinutes,
  val firstAction: MediaNotificationAction = NotificationPrefs().firstAction,
  val secondAction: MediaNotificationAction = NotificationPrefs().secondAction,
  val playbackSpeedCycle: List<Float> = NotificationPrefs().playbackSpeedCycle,
  val sleepTimerMode: SleepTimerNotificationMode = NotificationPrefs().sleepTimerMode,
  val sleepTimerCycle: List<Int> = NotificationPrefs().sleepTimerCycle,
  val playbackSpeedMode: PlaybackSpeedNotificationMode = NotificationPrefs().playbackSpeedMode,
  val playbackSpeedToggleTarget: Float = NotificationPrefs().playbackSpeedToggleTarget,
)
