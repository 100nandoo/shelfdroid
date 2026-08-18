package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.OpenIdSettingsSummary
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationSettingsContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun loadingState_isVisible() {
    composeRule.setContent { AuthenticationSettingsContent(state = AuthenticationSettingsState.Loading) }

    composeRule.onNodeWithText("Loading Authentication settings…").assertIsDisplayed()
  }

  @Test
  fun readyState_showsSettingsSummaryWithoutSecret() {
    composeRule.setContent {
      AuthenticationSettingsContent(
        state =
          AuthenticationSettingsState.Ready(
            AuthenticationSettingsSummary(
              customMessageEnabled = true,
              activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
              openId =
                OpenIdSettingsSummary(
                  issuerUrl = "https://issuer.example.com",
                  clientId = "shelfdroid",
                  clientSecretConfigured = true,
                  mobileRedirectUris = listOf("audiobookshelf://oauth"),
                  matchExistingBy = "email",
                ),
            )
          )
      )
    }

    composeRule.onNodeWithText("Authentication").assertIsDisplayed()
    composeRule.onNodeWithText("Enabled").assertIsDisplayed()
    composeRule.onNodeWithText("Username and password, OpenID login").assertIsDisplayed()
    composeRule.onNodeWithText("https://issuer.example.com").assertIsDisplayed()
    composeRule.onNodeWithText("Configured").assertIsDisplayed()
    composeRule.onNodeWithText("audiobookshelf://oauth").assertIsDisplayed()
    composeRule.onNodeWithText("email").assertIsDisplayed()
    composeRule.onAllNodesWithText("secret-value").assertCountEquals(0)
  }

  @Test
  fun failureState_showsMessageAndRetryAction() {
    var retried = false
    composeRule.setContent {
      AuthenticationSettingsContent(
        state = AuthenticationSettingsState.Failure("The server could not be reached."),
        onRetry = { retried = true },
      )
    }

    composeRule.onNodeWithText("Authentication settings unavailable").assertIsDisplayed()
    composeRule.onNodeWithText("The server could not be reached.").assertIsDisplayed()
    composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()

    assertTrue(retried)
  }

  @Test
  fun accessDeniedState_showsMessageAndBackAction() {
    var wentBack = false
    composeRule.setContent {
      AuthenticationSettingsContent(
        state = AuthenticationSettingsState.AccessDenied,
        onBackClicked = { wentBack = true },
      )
    }

    composeRule.onNodeWithText("Access denied").assertIsDisplayed()
    composeRule
      .onNodeWithText("Only admin and root Users can view Authentication settings.")
      .assertIsDisplayed()
    composeRule.onNodeWithText("Back").assertIsDisplayed().performClick()

    assertTrue(wentBack)
  }
}
