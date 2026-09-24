package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNotificationContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun notificationOpeningAndPresentationPrecedePreviewAndNotificationButtonsFollowSettings() {
    composeRule.setContent { SettingsNotificationContent() }

    val openingScreenTop =
      composeRule.onNodeWithText("Open notification in").fetchSemanticsNode().boundsInRoot.top
    val presentationTop =
      composeRule.onNodeWithText("Player presentation").fetchSemanticsNode().boundsInRoot.top
    val mediaNotificationTop =
      composeRule.onNodeWithText("Preview").fetchSemanticsNode().boundsInRoot.top
    val sleepTimerTop =
      composeRule.onNodeWithText("Sleep Timer").fetchSemanticsNode().boundsInRoot.top
    val playbackSpeedTop =
      composeRule.onNodeWithText("Playback speed cycle").fetchSemanticsNode().boundsInRoot.top
    val notificationButtonsTop =
      composeRule.onNodeWithText("Notification buttons").fetchSemanticsNode().boundsInRoot.top

    assertTrue(openingScreenTop < presentationTop)
    assertTrue(presentationTop < mediaNotificationTop)
    assertTrue(mediaNotificationTop < sleepTimerTop)
    assertTrue(sleepTimerTop < playbackSpeedTop)
    assertTrue(playbackSpeedTop < notificationButtonsTop)
  }

  @Test
  fun notificationTapSettingsExposeIndependentOpeningScreenAndPlayerPresentationOptions() {
    val events = mutableListOf<SettingsNotificationEvent>()
    composeRule.setContent { SettingsNotificationContent(onEvent = events::add) }

    composeRule.onNodeWithText("Home").performScrollTo().assertIsSelected()
    composeRule.onNodeWithText("Expanded player").performScrollTo().assertIsSelected()

    composeRule
      .onNodeWithText("Media details")
      .performScrollTo()
      .performClick()
    composeRule
      .onNodeWithText("Mini player")
      .performScrollTo()
      .performClick()

    assertEquals(
      listOf(
        SettingsNotificationEvent.ChangeOpeningScreen(MediaNotificationOpeningScreen.MediaDetails),
        SettingsNotificationEvent.ChangePlayerPresentation(
          MediaNotificationPlayerPresentation.MiniPlayer
        ),
      ),
      events,
    )
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
    composeRule.onNodeWithText("1").performScrollTo().assertIsNotEnabled()
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

    composeRule.onNodeWithText("1").performScrollTo().assertIsEnabled()
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

    composeRule.onNodeWithText("1").performScrollTo().assertIsEnabled()
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

    composeRule.onNodeWithText("1").performScrollTo().assertIsNotEnabled()
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

  @Test
  fun switchingSleepTimerModesShowsTheirControlsAndPreservesSelections() {
    val state = mutableStateOf(SettingsNotificationUiState(sleepTimerMinutes = 30))
    composeRule.setContent {
      SettingsNotificationContent(uiState = state.value) { event ->
        when (event) {
          is SettingsNotificationEvent.ChangeSleepTimerMode ->
            state.value = state.value.copy(sleepTimerMode = event.mode)
          is SettingsNotificationEvent.ChangeSleepTimerCycle ->
            state.value = state.value.copy(sleepTimerCycle = event.minutes)
          else -> Unit
        }
      }
    }

    composeRule.onNodeWithText("Toggle").performScrollTo().performClick()
    composeRule.onAllNodesWithText("Cyclical").get(1).performClick()
    composeRule.onNodeWithText("30").assertDoesNotExist()
    composeRule.onNodeWithText("1 min").performScrollTo().assertIsSelected()
    composeRule.onNodeWithText("5 min").performScrollTo().assertIsSelected()
    composeRule.onNodeWithText("10 min").performScrollTo().assertIsNotSelected().performClick()
    composeRule.onNodeWithText("10 min").assertIsSelected()
    composeRule.onNodeWithText("End of chapter").assertDoesNotExist()
    composeRule.onNodeWithText("Custom").assertDoesNotExist()

    composeRule.onAllNodesWithText("Cyclical").get(0).performScrollTo().performClick()
    composeRule.onNodeWithText("Toggle").performClick()
    composeRule.onNodeWithText("30").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("1 min").assertDoesNotExist()

    composeRule.onNodeWithText("Toggle").performScrollTo().performClick()
    composeRule.onAllNodesWithText("Cyclical").get(1).performClick()
    composeRule.onNodeWithText("10 min").performScrollTo().assertIsSelected()
  }

  @Test
  fun cyclicalModeRequiresOneDurationAndDisablesControlsWithoutTimerAction() {
    val state =
      mutableStateOf(
        SettingsNotificationUiState(
          sleepTimerMode = SleepTimerNotificationMode.Cyclical,
          sleepTimerCycle = listOf(5),
        )
      )
    composeRule.setContent { SettingsNotificationContent(uiState = state.value) }

    composeRule.onNodeWithText("5 min").performScrollTo().assertIsSelected().assertIsNotEnabled()
    composeRule.onNodeWithText("1 min").performScrollTo().assertIsEnabled()
    composeRule.onNodeWithText("60 min").performScrollTo().assertIsEnabled()

    state.value =
      state.value.copy(
        firstAction = MediaNotificationAction.None,
        secondAction = MediaNotificationAction.NextChapter,
      )
    composeRule
      .onAllNodesWithText("Cyclical")
      .get(0)
      .performScrollTo()
      .assertIsNotEnabled()
    composeRule.onNodeWithText("1 min").performScrollTo().assertIsNotEnabled()
    composeRule.onNodeWithText("60 min").performScrollTo().assertIsNotEnabled()
  }

  @Test
  fun mediaNotificationPreview_showsDummyMediaAndConfiguredActions() {
    composeRule.setContent {
      SettingsNotificationContent(
        uiState =
          SettingsNotificationUiState(
            firstAction = MediaNotificationAction.SleepTimer,
            secondAction = MediaNotificationAction.NextChapter,
          )
      )
    }

    composeRule.onNodeWithText("The Red-Headed League").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Sir Arthur Conan Doyle").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Seek back").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Play").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Seek forward").performScrollTo().assertIsDisplayed()
    composeRule
      .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.5f, 0f..1f)))
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Timer").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Next chapter").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun mediaNotificationPreview_updatesActionsAndOmitsNone() {
    val state =
      mutableStateOf(
        SettingsNotificationUiState(
          firstAction = MediaNotificationAction.None,
          secondAction = MediaNotificationAction.None,
        )
      )
    composeRule.setContent { SettingsNotificationContent(uiState = state.value) }

    composeRule.onAllNodesWithContentDescription("Timer").assertCountEquals(0)
    composeRule.onAllNodesWithContentDescription("Next chapter").assertCountEquals(0)

    state.value = state.value.copy(firstAction = MediaNotificationAction.PlaybackSpeed)
    composeRule.waitForIdle()

    composeRule.onNodeWithContentDescription("Playback speed").performScrollTo().assertIsDisplayed()
  }
}
