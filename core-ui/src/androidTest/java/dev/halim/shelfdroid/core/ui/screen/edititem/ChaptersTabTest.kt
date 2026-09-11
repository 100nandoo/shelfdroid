package dev.halim.shelfdroid.core.ui.screen.edititem

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.edititem.ChapterSourceTrack
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ChaptersTab
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChaptersTabTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun setChaptersButton_isDisabledWithoutEligibleTracks() {
    composeRule.setContent { ChaptersTab(uiState = EditItemUiState(), onEvent = {}) }

    composeRule.onNodeWithText("Set Chapters from Tracks").assertIsNotEnabled()
  }

  @Test
  fun setChaptersButton_confirmsBeforeDispatchingEvent() {
    val events = mutableListOf<EditItemEvent>()
    val state =
      EditItemUiState(
        chapterTracks = listOf(ChapterSourceTrack(filename = "track.mp3", duration = 30.0))
      )
    composeRule.setContent { ChaptersTab(uiState = state, onEvent = events::add) }

    composeRule.onNodeWithText("Set Chapters from Tracks").assertIsEnabled().performClick()
    composeRule
      .onNodeWithText(
        "This will replace the existing chapters with one chapter for each eligible audio track."
      )
      .assertIsDisplayed()
    composeRule.onNodeWithText("Set Chapters").performClick()

    assertTrue(events.contains(EditItemEvent.SetChaptersFromTracks))
  }

  @Test
  fun setChaptersButton_showsProgressAndDisablesActionsWhileSaving() {
    val state =
      EditItemUiState(
        isSaving = true,
        isSettingChapters = true,
        chapterTracks = listOf(ChapterSourceTrack(filename = "track.mp3", duration = 30.0)),
      )
    composeRule.setContent { ChaptersTab(uiState = state, onEvent = {}) }

    composeRule.onNodeWithText("Edit Chapters").assertIsNotEnabled()
    composeRule.onNodeWithTag("set-chapters-button").assertIsNotEnabled()
    composeRule.onAllNodesWithText("Set Chapters from Tracks").assertCountEquals(0)
  }
}
