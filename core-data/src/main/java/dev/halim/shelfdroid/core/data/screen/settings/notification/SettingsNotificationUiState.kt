package dev.halim.shelfdroid.core.data.screen.settings.notification

import dev.halim.shelfdroid.core.MediaNotificationAction
import dev.halim.shelfdroid.core.NotificationPrefs

data class SettingsNotificationUiState(
  val sleepTimerMinutes: Int = 15,
  val firstAction: MediaNotificationAction = NotificationPrefs().firstAction,
  val secondAction: MediaNotificationAction = NotificationPrefs().secondAction,
  val playbackSpeedCycle: List<Float> = NotificationPrefs().playbackSpeedCycle,
)
