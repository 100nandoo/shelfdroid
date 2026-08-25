package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryAdministrationContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun loadingState_isVisibleAndRefreshIsAccessible() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState = LibraryAdministrationUiState(),
      )
    }

    composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Refresh Libraries").assertIsDisplayed()
  }

  @Test
  fun failureState_showsRetryAndDoesNotShowLibraryNames() {
    var refreshes = 0
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Failure("offline"),
            isRefreshing = false,
          ),
        onEvent = { refreshes += 1 },
      )
    }

    composeRule.onNodeWithText("offline").assertIsDisplayed()
    composeRule.onNodeWithText("Retry").performClick()
    composeRule.onAllNodesWithText("Books").assertCountEquals(0)
    assertEquals(1, refreshes)
  }

  @Test
  fun emptyState_isVisible() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState = LibraryAdministrationUiState(state = GenericState.Success, isRefreshing = false)
      )
    }

    composeRule.onNodeWithText("No Libraries are available.").assertIsDisplayed()
  }

  @Test
  fun orderedLibraries_showTypeAndIdentityAndRowsAreInert() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "podcasts",
                  name = "Podcasts",
                  mediaType = LibraryAdministrationMediaType.PODCAST,
                  displayOrder = 0,
                ),
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                ),
              ),
          )
      )
    }

    composeRule.onNodeWithText("Podcasts").assertIsDisplayed().assertHasNoClickAction()
    composeRule.onNodeWithText("Podcast Library").assertIsDisplayed()
    composeRule.onNodeWithText("Library ID: podcasts").assertIsDisplayed()
    composeRule.onNodeWithText("Books").assertIsDisplayed().assertHasNoClickAction()
    composeRule.onNodeWithText("Book Library").assertIsDisplayed()
    composeRule.onNodeWithText("Library ID: books").assertIsDisplayed()
  }
}
