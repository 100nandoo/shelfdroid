package dev.halim.shelfdroid.core.ui.screen.login

import dev.halim.shelfdroid.core.AuthPromptReason
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
            loginDiscoveryMessage = "Could not confirm this server's login methods.",
          ),
        refreshFailedMessage = "Failed to refresh token. Re-login required.",
        manualReLoginMessage = "Re-enter your password to continue.",
      )

    assertEquals(
      listOf(
        "Failed to refresh token. Re-login required.",
        "Sign in again to continue.",
        "Could not confirm this server's login methods.",
      ),
      messages.ordered(),
    )
  }
}
