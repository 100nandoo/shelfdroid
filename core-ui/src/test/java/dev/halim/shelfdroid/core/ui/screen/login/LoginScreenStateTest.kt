package dev.halim.shelfdroid.core.ui.screen.login

import dev.halim.shelfdroid.core.AuthPromptReason
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
}

private fun testDiscoveryMessageText(message: LoginDiscoveryMessage): String {
  return when (message) {
    LoginDiscoveryMessage.MethodsUnconfirmed ->
      "Could not confirm this server's login methods. You can still try signing in with your username and password."
    LoginDiscoveryMessage.LocalLoginUnavailable ->
      "This server requires OpenID login. Use OpenID login to continue."
  }
}
