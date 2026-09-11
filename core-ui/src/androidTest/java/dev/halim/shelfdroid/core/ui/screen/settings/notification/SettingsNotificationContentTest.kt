package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNotificationContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun sections_areOrderedWithMediaNotificationActionsAtTheBottom() {
    composeRule.setContent { SettingsNotificationContent() }

    val sleepTimerTop =
      composeRule.onNodeWithText("Sleep Timer").fetchSemanticsNode().boundsInRoot.top
    val playbackSpeedTop =
      composeRule.onNodeWithText("Playback speed cycle").fetchSemanticsNode().boundsInRoot.top
    val mediaNotificationTop =
      composeRule.onNodeWithText("Media notification").fetchSemanticsNode().boundsInRoot.top

    assertTrue(sleepTimerTop < playbackSpeedTop)
    assertTrue(playbackSpeedTop < mediaNotificationTop)
  }

  @Test
  fun disabledActions_disableTheirConfigurationSections() {
    composeRule.setContent {
      SettingsNotificationContent(
        uiState =
          SettingsNotificationUiState(
            firstAction = MediaNotificationAction.None,
            secondAction = MediaNotificationAction.NextChapter,
          )
      )
    }

    composeRule.onNodeWithText("Sleep Timer").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("15").performScrollTo().assertIsNotEnabled()
    composeRule.onNodeWithText("Playback speed cycle").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("0.5x").performScrollTo().assertIsNotEnabled()
    composeRule.onNodeWithText("None").performScrollTo().assertIsEnabled()
  }

  @Test
  fun sleepTimerAction_enablesSleepTimerConfigurationOnly() {
    composeRule.setContent {
      SettingsNotificationContent(
        uiState =
          SettingsNotificationUiState(
            firstAction = MediaNotificationAction.SleepTimer,
            secondAction = MediaNotificationAction.PreviousChapter,
          )
      )
    }

    composeRule.onNodeWithText("15").performScrollTo().assertIsEnabled()
    composeRule.onNodeWithText("0.5x").performScrollTo().assertIsNotEnabled()
  }

  @Test
  fun sleepTimerActionInSecondSlot_enablesSleepTimerConfiguration() {
    composeRule.setContent {
      SettingsNotificationContent(
        uiState =
          SettingsNotificationUiState(
            firstAction = MediaNotificationAction.NextChapter,
            secondAction = MediaNotificationAction.SleepTimer,
          )
      )
    }

    composeRule.onNodeWithText("15").performScrollTo().assertIsEnabled()
  }

  @Test
  fun playbackSpeedAction_enablesPlaybackSpeedConfigurationOnly() {
    composeRule.setContent {
      SettingsNotificationContent(
        uiState =
          SettingsNotificationUiState(
            firstAction = MediaNotificationAction.PlaybackSpeed,
            secondAction = MediaNotificationAction.None,
          )
      )
    }

    composeRule.onNodeWithText("15").performScrollTo().assertIsNotEnabled()
    composeRule.onNodeWithText("0.5x").performScrollTo().assertIsEnabled()
  }

  @Test
  fun playbackSpeedActionInSecondSlot_enablesPlaybackSpeedConfiguration() {
    composeRule.setContent {
      SettingsNotificationContent(
        uiState =
          SettingsNotificationUiState(
            firstAction = MediaNotificationAction.NextChapter,
            secondAction = MediaNotificationAction.PlaybackSpeed,
          )
      )
    }

    composeRule.onNodeWithText("0.5x").performScrollTo().assertIsEnabled()
  }
}
