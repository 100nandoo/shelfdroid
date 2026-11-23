package dev.halim.shelfdroid.core.ui.screen.settings.notification

import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.PlaybackSpeedNotificationMode
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.screen.settings.search.SettingsSearchSettingId
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsNotificationSearchTargetTest {
  @Test
  fun hiddenSleepTimerDurationPointsToModeWithoutChangingIt() {
    val uiState =
      SettingsNotificationUiState(
        firstAction = MediaNotificationAction.SleepTimer,
        sleepTimerMode = SleepTimerNotificationMode.Cyclical,
      )

    val resolution =
      resolveNotificationSearchTarget(
        SettingsSearchSettingId.DEFAULT_SLEEP_TIMER,
        uiState,
        sleepTimerEnabled = true,
        playbackSpeedEnabled = false,
      )

    assertEquals(SettingsSearchSettingId.SLEEP_TIMER_MODE, resolution.targetSettingId)
    assertEquals(R.string.settings_search_choose_sleep_timer_toggle, resolution.guidanceResId)
    assertEquals(SleepTimerNotificationMode.Cyclical, uiState.sleepTimerMode)
  }

  @Test
  fun unavailableSleepTimerCyclePointsToAnEmptyActionSlot() {
    val uiState =
      SettingsNotificationUiState(
        firstAction = MediaNotificationAction.PlaybackSpeed,
        secondAction = MediaNotificationAction.None,
      )

    val resolution =
      resolveNotificationSearchTarget(
        SettingsSearchSettingId.SLEEP_TIMER_CYCLE,
        uiState,
        sleepTimerEnabled = false,
        playbackSpeedEnabled = true,
      )

    assertEquals(SettingsSearchSettingId.NOTIFICATION_ACTION_TWO, resolution.targetSettingId)
    assertEquals(R.string.settings_search_assign_sleep_timer_action, resolution.guidanceResId)
  }

  @Test
  fun unavailablePlaybackSpeedModePointsToAnEmptyActionSlot() {
    val uiState =
      SettingsNotificationUiState(
        firstAction = MediaNotificationAction.SleepTimer,
        secondAction = MediaNotificationAction.None,
      )

    val resolution =
      resolveNotificationSearchTarget(
        SettingsSearchSettingId.PLAYBACK_SPEED_MODE,
        uiState,
        sleepTimerEnabled = true,
        playbackSpeedEnabled = false,
      )

    assertEquals(SettingsSearchSettingId.NOTIFICATION_ACTION_TWO, resolution.targetSettingId)
    assertEquals(R.string.settings_search_assign_playback_speed_action, resolution.guidanceResId)
  }

  @Test
  fun visiblePlaybackSpeedTargetPointsToItsControl() {
    val uiState =
      SettingsNotificationUiState(
        firstAction = MediaNotificationAction.PlaybackSpeed,
        playbackSpeedMode = PlaybackSpeedNotificationMode.Toggle,
      )

    val resolution =
      resolveNotificationSearchTarget(
        SettingsSearchSettingId.PLAYBACK_SPEED_TARGET,
        uiState,
        sleepTimerEnabled = false,
        playbackSpeedEnabled = true,
      )

    assertEquals(SettingsSearchSettingId.PLAYBACK_SPEED_TARGET, resolution.targetSettingId)
    assertEquals(null, resolution.guidanceResId)
  }
}
