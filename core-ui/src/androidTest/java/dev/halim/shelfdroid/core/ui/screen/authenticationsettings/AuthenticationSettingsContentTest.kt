package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
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
    composeRule.onNodeWithText("Configured").assertExists()
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

  @Test
  fun editorState_preservesHtmlPreviewAndDisablesCleanSave() {
    val settings =
      AuthenticationSettingsSummary(
        customMessageEnabled = true,
        customMessage = "<p>Welcome <strong>back</strong></p>",
        activeLoginMethods = listOf(LoginMethod.Local),
      )
    val uiState =
      AuthenticationSettingsUiState(
        state = AuthenticationSettingsState.Ready(settings),
        savedSettings = settings,
        draftSettings = settings,
        restartRequired = true,
      )

    composeRule.setContent {
      AuthenticationSettingsContent(
        state = uiState.state,
        uiState = uiState,
      )
    }

    composeRule.onNodeWithText("Custom message HTML").assertIsDisplayed()
    composeRule.onNodeWithText("<p>Welcome <strong>back</strong></p>").assertIsDisplayed()
    composeRule.onNodeWithText("Welcome back", substring = true).assertExists()
    composeRule.onNodeWithText("Issuer URL").assertIsDisplayed()
    composeRule.onNodeWithText("Authorization URL").assertIsDisplayed()
    composeRule.onNodeWithText("Callbacks").assertExists()
    composeRule.onNodeWithText("New mobile redirect URI").assertExists()
    composeRule.onNodeWithText("Effective web callback URL").assertExists()
    composeRule.onNodeWithText("https://audiobooks.dev/auth/openid/callback").assertExists()
    composeRule.onNodeWithText("Effective mobile callback URL").assertExists()
    composeRule
      .onNodeWithText("https://audiobooks.dev/auth/openid/mobile-redirect")
      .assertExists()
    composeRule
      .onNodeWithText("Restart the Audiobookshelf server for all OpenID changes to take effect.")
      .assertExists()
    composeRule.onNodeWithText("Save").assertIsNotEnabled()
  }

  @Test
  fun editorState_masksReplacementAndDoesNotExposeSecretText() {
    val settings =
      AuthenticationSettingsSummary(
        activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
        openId =
          OpenIdSettingsSummary(
            issuerUrl = "https://issuer.example.com",
            clientId = "shelfdroid",
            clientSecretConfigured = true,
          ),
      )
    val uiState =
      AuthenticationSettingsUiState(
        state = AuthenticationSettingsState.Ready(settings),
        savedSettings = settings,
        draftSettings = settings,
      )

    composeRule.setContent {
      AuthenticationSettingsContent(
        state = uiState.state,
        uiState = uiState,
        clientSecretReplacement = "secret-value",
      )
    }

    composeRule.onNodeWithText("Configured").assertExists()
    composeRule.onNodeWithText("Clear client secret").assertExists()
    composeRule.onAllNodesWithText("secret-value").assertCountEquals(0)
  }
}
