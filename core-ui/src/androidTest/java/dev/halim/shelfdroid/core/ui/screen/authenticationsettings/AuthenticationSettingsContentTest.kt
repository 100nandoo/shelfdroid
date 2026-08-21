package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
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
    composeRule.setContent {
      AuthenticationSettingsContent(state = AuthenticationSettingsState.Loading)
    }

    composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
  }

  @Test
  fun readyState_showsClientSecretFieldWithoutExposingSecretText() {
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
                  clientSecret = "secret-value",
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
    composeRule.onNodeWithText("Client secret").assertIsDisplayed()
    composeRule.onNodeWithText("audiobookshelf://oauth").assertIsDisplayed()
    composeRule.onNodeWithText("email").assertIsDisplayed()
    composeRule.onAllNodesWithText("secret-value").assertCountEquals(0)
  }

  @Test
  fun issuerUrlImeNext_focusesAndRevealsAuthorizationUrl() {
    val settings =
      AuthenticationSettingsSummary(
        customMessageEnabled = true,
        customMessage = "Welcome to ShelfDroid",
        activeLoginMethods = listOf(LoginMethod.OpenId),
        openId = OpenIdSettingsSummary(issuerUrl = "https://issuer.example.com"),
      )
    val uiState =
      AuthenticationSettingsUiState(
        state = AuthenticationSettingsState.Ready(settings),
        savedSettings = settings,
        draftSettings = settings,
      )

    var imeBottom = 0
    var showSoftwareKeyboard: (() -> Unit)? = null
    var hideSoftwareKeyboard: (() -> Unit)? = null
    composeRule.setContent {
      imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
      val softwareKeyboardController = LocalSoftwareKeyboardController.current
      showSoftwareKeyboard = { softwareKeyboardController?.show() }
      hideSoftwareKeyboard = { softwareKeyboardController?.hide() }
      AuthenticationSettingsContent(state = uiState.state, uiState = uiState)
    }

    composeRule.onNodeWithText("Issuer URL").performScrollTo().performClick()
    composeRule.runOnIdle { showSoftwareKeyboard?.invoke() }
    composeRule.waitUntil(timeoutMillis = 5_000) { imeBottom > 0 }
    composeRule.onNodeWithText("Issuer URL").performImeAction()

    val authorizationUrl =
      composeRule.onNodeWithText("Authorization URL").assertIsFocused().assertIsDisplayed()
    val visibleBottom = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.bottom - imeBottom
    val authorizationBottom = authorizationUrl.fetchSemanticsNode().boundsInRoot.bottom

    assertTrue(authorizationBottom <= visibleBottom)
    composeRule.runOnIdle { hideSoftwareKeyboard?.invoke() }
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

    composeRule.onAllNodesWithText("Authentication settings unavailable").assertCountEquals(0)
    composeRule.onNodeWithText("The server could not be reached.").assertIsDisplayed()
    composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()

    assertTrue(retried)
  }

  @Test
  fun editorState_preservesHtmlPreviewAndDisablesCleanSave() {
    val settings =
      AuthenticationSettingsSummary(
        customMessageEnabled = true,
        customMessage = "<p>Welcome <strong>back</strong></p>",
        activeLoginMethods = listOf(LoginMethod.Local),
        openId =
          OpenIdSettingsSummary(
            buttonText = "Continue with Acme",
            matchExistingBy = "email",
            autoLaunch = true,
            autoRegister = true,
            groupClaim = "groups",
            advancedPermsClaim = "abspermissions",
            samplePermissions = "{\"download\":true}",
          ),
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
    composeRule.onNodeWithText("https://audiobooks.dev/auth/openid/mobile-redirect").assertExists()
    composeRule.onNodeWithText("User mapping").assertExists()
    composeRule.onNodeWithText("OpenID button text").assertExists()
    composeRule.onNodeWithText("Match existing Users by").assertExists()
    composeRule.onNodeWithText("None").assertExists()
    composeRule.onNodeWithText("Email").assertExists()
    composeRule.onNodeWithText("Username").assertExists()
    composeRule.onNodeWithText("Automatic OpenID launch").assertExists()
    composeRule.onNodeWithText("Automatic User registration").assertExists()
    composeRule.onNodeWithText("Group claim").assertExists()
    composeRule.onNodeWithText("Advanced permissions claim").assertExists()
    composeRule.onNodeWithText("Sample permissions").assertExists()
    composeRule.onNodeWithText("download", substring = true).assertExists()
    composeRule
      .onNodeWithText("Restart the Audiobookshelf server for all OpenID changes to take effect.")
      .assertExists()
    composeRule.onNodeWithText("Save").assertIsNotEnabled()
  }

  @Test
  fun editorState_masksLoadedSecretAndDoesNotExposeSecretText() {
    val settings =
      AuthenticationSettingsSummary(
        activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
        openId =
          OpenIdSettingsSummary(
            issuerUrl = "https://issuer.example.com",
            clientId = "shelfdroid",
            clientSecret = "secret-value",
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
      )
    }

    composeRule.onNodeWithText("Client secret").assertIsDisplayed()
    composeRule.onAllNodesWithText("secret-value").assertCountEquals(0)
  }

  @Test
  fun editorActions_haveAccessibleLabels() {
    val settings =
      AuthenticationSettingsSummary(
        activeLoginMethods = listOf(LoginMethod.Local),
        openId = OpenIdSettingsSummary(mobileRedirectUris = listOf("audiobookshelf://oauth")),
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
      )
    }

    composeRule.onNodeWithContentDescription("Username and password").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("OpenID login").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Remove mobile redirect URI 1").assertExists()
  }
}
