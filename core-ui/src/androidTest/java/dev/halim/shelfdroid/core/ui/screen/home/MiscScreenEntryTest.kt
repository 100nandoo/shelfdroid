package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    val isAdmin = mutableStateOf(true)
    composeRule.setContent {
      MiscScreen(
        isAdmin = isAdmin.value,
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

    composeRule.runOnIdle { isAdmin.value = false }

    composeRule.onAllNodesWithText("Libraries").assertCountEquals(0)
    assertEquals(1, clickCount)
  }

  @Test
  fun clientSettings_isReachableAfterScrollingAdminEntries() {
    composeRule.setContent {
      MiscScreen(
        isAdmin = true,
        onUsersClicked = {},
        onLibrariesClicked = {},
        onApiKeysClicked = {},
        onServerSettingsClicked = {},
        onEmailManagementClicked = {},
        onAppriseNotificationSettingsClicked = {},
        onRssFeedsClicked = {},
        onLogsClicked = {},
        onBackupsClicked = {},
      )
    }

    composeRule.onNode(hasScrollAction()).assertExists()
    composeRule.onAllNodesWithText("Settings").assertCountEquals(2)
    composeRule.onAllNodesWithText("Settings")[1].performScrollTo().assertIsDisplayed()
  }
}
