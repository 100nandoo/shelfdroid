package dev.halim.shelfdroid.core.ui.screen.settings.widget

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsWidgetScreenTest {
  @get:Rule val composeRule = createComposeRule()

  @Test
  fun rejectedPinRequestShowsErrorSnackbar() {
    val snackbarHostState = SnackbarHostState()
    var requestPinCalled = false
    composeRule.setContent {
      SettingsWidgetScreen(snackbarHostState = snackbarHostState) {
        requestPinCalled = true
        false
      }
    }

    composeRule.onNodeWithText("Add widget").performClick()
    composeRule.waitUntil(1_000) { snackbarHostState.currentSnackbarData != null }

    assertEquals("Could not add widget", snackbarHostState.currentSnackbarData?.visuals?.message)
    composeRule.runOnIdle { assertEquals(true, requestPinCalled) }
  }

  @Test
  fun acceptedPinRequestDoesNotShowSnackbar() {
    val snackbarHostState = SnackbarHostState()
    composeRule.setContent {
      SettingsWidgetScreen(snackbarHostState = snackbarHostState) { true }
    }

    composeRule.onNodeWithText("Add widget").performClick()
    composeRule.waitForIdle()

    assertNull(snackbarHostState.currentSnackbarData)
  }
}
