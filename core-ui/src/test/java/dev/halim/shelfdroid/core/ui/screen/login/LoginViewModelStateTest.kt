package dev.halim.shelfdroid.core.ui.screen.login

import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryMessage
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryResult
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailure
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginStartResult
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.isOpenIdOnly
import dev.halim.shelfdroid.core.data.screen.login.showsMixedLoginMethods
import dev.halim.shelfdroid.core.data.screen.login.supportsLocalLogin
import dev.halim.shelfdroid.core.ui.navigation.Login
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelStateTest {

  @Test
  fun initLoginUiState_whenForcedReloginHasSavedServer_startsFreshDiscovery() {
    val initialized =
      initLoginUiState(
        navKey = Login(reLogin = true, reason = AuthPromptReason.RefreshFailed),
        username = "fernando",
        server = "https://example.com/audiobookshelf/",
      )

    assertTrue(initialized.reLogin)
    assertEquals(AuthPromptReason.RefreshFailed, initialized.authPromptReason)
    assertEquals("fernando", initialized.username)
    assertEquals("https://example.com/audiobookshelf/", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(LoginDiscoveryState.Loading, initialized.discoveryState)
    assertEquals(listOf(LoginMethod.Local), initialized.availableLoginMethods)
  }

  @Test
  fun initLoginUiState_whenOpenIdCallbackFailureExists_prefillsServerAndShowsFailure() {
    val initialized =
      initLoginUiState(
        navKey = Login(),
        openIdLoginFailure =
          OpenIdLoginFailure(
            normalizedServer = "https://example.com/audiobookshelf",
            errorMessage = "OpenID login failed because the callback state does not match the current login.",
          ),
      )

    assertEquals("https://example.com/audiobookshelf", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(LoginDiscoveryState.Loading, initialized.discoveryState)
    assertTrue(initialized.loginState is GenericState.Failure)
    assertEquals(
      "OpenID login failed because the callback state does not match the current login.",
      (initialized.loginState as GenericState.Failure).errorMessage,
    )
  }

  @Test
  fun prepareLoginDiscovery_whenServerChanges_clearsStaleDiscoveryState() {
    val prepared =
      LoginUiState(
          server = "https://old.example.com",
          normalizedServer = "https://old.example.com",
          discoveryState = LoginDiscoveryState.Success,
          availableLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
          loginDiscoveryMessage = LoginDiscoveryMessage.MethodsUnconfirmed,
          authLoginCustomMessage = "stale custom copy",
          authOpenIdButtonText = "Stale SSO",
          authOpenIdAutoLaunch = true,
        )
        .prepareLoginDiscovery("https://new.example.com")

    assertEquals("https://new.example.com", prepared.server)
    assertEquals("https://new.example.com", prepared.normalizedServer)
    assertEquals(LoginDiscoveryState.Loading, prepared.discoveryState)
    assertEquals(listOf(LoginMethod.Local), prepared.availableLoginMethods)
    assertNull(prepared.loginDiscoveryMessage)
    assertNull(prepared.authLoginCustomMessage)
    assertNull(prepared.authOpenIdButtonText)
    assertNull(prepared.authOpenIdAutoLaunch)
  }

  @Test
  fun applyLoginDiscovery_whenBothMethodsExist_defaultsToLocalLoginSurface() {
    val applied =
      LoginUiState(server = "https://example.com")
        .applyLoginDiscovery(
          LoginDiscoveryResult(
            normalizedServer = "https://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
            authOpenIdButtonText = "Continue with Acme SSO",
          )
        )

    assertEquals("Continue with Acme SSO", applied.authOpenIdButtonText)
    assertTrue(applied.showsMixedLoginMethods())
    assertTrue(applied.supportsLocalLogin())
  }

  @Test
  fun applyLoginDiscovery_whenOnlyOpenIdExists_switchesToOpenIdSurface() {
    val applied =
      LoginUiState(server = "https://example.com")
        .applyLoginDiscovery(
          LoginDiscoveryResult(
            normalizedServer = "https://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.OpenId),
            loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
            authOpenIdButtonText = "Continue with Acme SSO",
          )
        )

    assertEquals("Continue with Acme SSO", applied.authOpenIdButtonText)
    assertTrue(applied.isOpenIdOnly())
  }

  @Test
  fun handleOpenIdLoginButtonPressed_emitsLaunchEventAndUpdatesUiState() = runBlocking {
    val events = mutableListOf<LoginUiEvent>()

    val updated =
      handleOpenIdLoginButtonPressed(
        uiState =
          LoginUiState(
            server = "https://example.com",
            normalizedServer = "https://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.OpenId),
            authOpenIdButtonText = "Continue with Acme SSO",
          ),
        redirectUri = "dev.halim.shelfdroid.debug://oauth",
        startOpenIdLogin = { uiState, _ ->
          OpenIdLoginStartResult(
            uiState = uiState.copy(server = "https://example.com", normalizedServer = "https://example.com"),
            authorizationUrl = "https://example.com/auth/openid?redirect_uri=dev.halim.shelfdroid.debug://oauth",
          )
        },
        emitEvent = { events += it },
      )

    assertEquals("https://example.com", updated.server)
    assertEquals("https://example.com", updated.normalizedServer)
    assertEquals(1, events.size)
    val event = events.single()
    assertTrue(event is LoginUiEvent.LaunchOpenIdLogin)
    assertEquals(
      "https://example.com/auth/openid?redirect_uri=dev.halim.shelfdroid.debug://oauth",
      (event as LoginUiEvent.LaunchOpenIdLogin).authorizationUrl,
    )
  }
}
