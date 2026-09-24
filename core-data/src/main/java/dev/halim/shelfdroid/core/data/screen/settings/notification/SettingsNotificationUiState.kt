package dev.halim.shelfdroid.core.data.screen.settings.notification

import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.PlaybackSpeedNotificationMode
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode

data class SettingsNotificationUiState(
  val playerPresentation: MediaNotificationPlayerPresentation =
    NotificationPrefs().playerPresentation,
  val openingScreen: MediaNotificationOpeningScreen = NotificationPrefs().openingScreen,
  val sleepTimerMinutes: Int = NotificationPrefs().sleepTimerMinutes,
  val firstAction: MediaNotificationAction = NotificationPrefs().firstAction,
  val secondAction: MediaNotificationAction = NotificationPrefs().secondAction,
  val playbackSpeedCycle: List<Float> = NotificationPrefs().playbackSpeedCycle,
  val sleepTimerMode: SleepTimerNotificationMode = NotificationPrefs().sleepTimerMode,
  val sleepTimerCycle: List<Int> = NotificationPrefs().sleepTimerCycle,
  val playbackSpeedMode: PlaybackSpeedNotificationMode = NotificationPrefs().playbackSpeedMode,
  val playbackSpeedToggleTarget: Float = NotificationPrefs().playbackSpeedToggleTarget,
)
