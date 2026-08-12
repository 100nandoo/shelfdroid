package dev.halim.shelfdroid.core.ui.screen.login

import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.data.screen.login.LocalNetworkPermissionState
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryMessage
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginScreenStateTest {

  @Test
  fun loginHeaderMessages_whenForcedReloginReasonAndServerMessageExist_keepsReasonFirst() {
    val messages =
      loginHeaderMessages(
        uiState =
          LoginUiState(
            reLogin = true,
            authPromptReason = AuthPromptReason.RefreshFailed,
            authLoginCustomMessage = "Sign in again to continue.",
            loginDiscoveryMessage = LoginDiscoveryMessage.MethodsUnconfirmed,
          ),
        refreshFailedMessage = "Failed to refresh token. Re-login required.",
        manualReLoginMessage = "Re-enter your password to continue.",
      )

    assertEquals(
      listOf(
        "Failed to refresh token. Re-login required.",
        "Sign in again to continue.",
        "Could not confirm this server's login methods. You can still try signing in with your username and password.",
      ),
      messages.ordered(::testDiscoveryMessageText),
    )
  }

  @Test
  fun withoutServerTextFieldSpacing_removesSpacesTabsAndNewlines() {
    assertEquals(
      "192.168.50.150:13378",
      " 192.168.50.150:\t13378\n ".withoutServerTextFieldSpacing(),
    )
  }

  @Test
  fun withoutLoginTextFieldNewlines_removesLineFeedAndCarriageReturn() {
    assertEquals(
      "username",
      "user\n\rname".withoutLoginTextFieldNewlines(),
    )
  }

  @Test
  fun containsLoginTextFieldNewline_detectsLineFeedAndCarriageReturn() {
    assertEquals(
      true,
      "user\nname".containsLoginTextFieldNewline(),
    )
    assertEquals(
      true,
      "user\rname".containsLoginTextFieldNewline(),
    )
    assertEquals(
      false,
      "username".containsLoginTextFieldNewline(),
    )
  }

  @Test
  fun localNetworkPermissionGuidance_whenDeniedOnce_showsMessageWithoutSettingsButton() {
    val guidance =
      localNetworkPermissionGuidance(
        uiState = LoginUiState(localNetworkPermissionState = LocalNetworkPermissionState.Denied),
        deniedMessage = "Allow local network access to continue.",
        permanentlyDeniedMessage = "Open settings to allow local network access.",
      )

    assertEquals("Allow local network access to continue.", guidance.message)
    assertEquals(false, guidance.showSettingsButton)
  }

  @Test
  fun localNetworkPermissionGuidance_whenPermanentlyDenied_showsSettingsButton() {
    val guidance =
      localNetworkPermissionGuidance(
        uiState =
          LoginUiState(localNetworkPermissionState = LocalNetworkPermissionState.PermanentlyDenied),
        deniedMessage = "Allow local network access to continue.",
        permanentlyDeniedMessage = "Open settings to allow local network access.",
      )

    assertEquals("Open settings to allow local network access.", guidance.message)
    assertEquals(true, guidance.showSettingsButton)
  }

  @Test
  fun serverAccessControlState_includesInternetAndLocalNetworkOptionsInOrder() {
    val controlState =
      serverAccessControlState(
        uiState = LoginUiState(),
        internetLabel = "Internet",
        localNetworkLabel = "Local network",
      )

    assertEquals(
      listOf(
        ServerAccessOption(ServerAccessMode.Internet, "Internet"),
        ServerAccessOption(ServerAccessMode.LocalNetwork, "Local network"),
      ),
      controlState.options,
    )
    assertEquals(true, controlState.enabled)
  }

  @Test
  fun serverAccessControlState_whenForcedRelogin_disablesSelector() {
    val controlState =
      serverAccessControlState(
        uiState = LoginUiState(reLogin = true),
        internetLabel = "Internet",
        localNetworkLabel = "Local network",
      )

    assertEquals(false, controlState.enabled)
  }
}

private fun testDiscoveryMessageText(message: LoginDiscoveryMessage): String {
  return when (message) {
    LoginDiscoveryMessage.MethodsUnconfirmed ->
      "Could not confirm this server's login methods. You can still try signing in with your username and password."
    LoginDiscoveryMessage.MethodsUnconfirmedTryLocalNetwork ->
      "Could not confirm how this server accepts sign-ins. If this Audiobookshelf server is on your local network, switch Server access to Local network and try again. You can still try signing in with your username and password."
    LoginDiscoveryMessage.LocalLoginUnavailable ->
      "This server requires OpenID login. Use OpenID login to continue."
  }
}
