package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiscScreenEntryTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun librariesEntry_isNavigableForAdminsAndHiddenForOtherUsers() {
    var clickCount = 0
    composeRule.setContent {
      MiscScreen(
        isAdmin = true,
        onUsersClicked = {},
        onLibrariesClicked = { clickCount += 1 },
        onApiKeysClicked = {},
        onServerSettingsClicked = {},
        onEmailManagementClicked = {},
        onAppriseNotificationSettingsClicked = {},
        onRssFeedsClicked = {},
        onLogsClicked = {},
        onBackupsClicked = {},
      )
    }

    composeRule.onNodeWithText("Libraries").assertIsDisplayed().performClick()
    assertEquals(1, clickCount)

    composeRule.setContent {
      MiscScreen(
        isAdmin = false,
        onUsersClicked = {},
        onLibrariesClicked = { clickCount += 1 },
        onApiKeysClicked = {},
        onServerSettingsClicked = {},
        onEmailManagementClicked = {},
        onAppriseNotificationSettingsClicked = {},
        onRssFeedsClicked = {},
        onLogsClicked = {},
        onBackupsClicked = {},
      )
    }

    composeRule.onAllNodesWithText("Libraries").assertCountEquals(0)
    assertEquals(1, clickCount)
  }
}
