package dev.halim.shelfdroid.core.ui.screen.login

import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryMessage
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryResult
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginCompletionResult
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailure
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginRecoveryState
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginStartResult
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
  fun initLoginUiState_whenFreshLoginDefaultsToInternetAccessMode() {
    val initialized =
      initLoginUiState(
        navKey = Login(),
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
      )

    assertEquals(ServerAccessMode.Internet, initialized.serverAccessMode)
  }

  @Test
  fun initLoginUiState_whenForcedReloginHasSavedServer_preparesDiscoveryWithoutLoading() {
    val initialized =
      initLoginUiState(
        navKey = Login(reLogin = true, reason = AuthPromptReason.RefreshFailed),
        username = "fernando",
        server = "https://example.com/audiobookshelf/",
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
      )

    assertTrue(initialized.reLogin)
    assertEquals(AuthPromptReason.RefreshFailed, initialized.authPromptReason)
    assertEquals("fernando", initialized.username)
    assertEquals("https://example.com/audiobookshelf/", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(ServerAccessMode.LocalNetwork, initialized.serverAccessMode)
    assertEquals(LoginDiscoveryState.Idle, initialized.discoveryState)
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
            errorMessage =
              "OpenID login failed because the callback state does not match the current login.",
          ),
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
      )

    assertEquals("https://example.com/audiobookshelf", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(ServerAccessMode.LocalNetwork, initialized.serverAccessMode)
    assertEquals(LoginDiscoveryState.Idle, initialized.discoveryState)
    assertTrue(initialized.loginState is GenericState.Failure)
    assertEquals(
      "OpenID login failed because the callback state does not match the current login.",
      (initialized.loginState as GenericState.Failure).errorMessage,
    )
  }

  @Test
  fun initLoginUiState_whenPendingOpenIdRecoveryExists_prefillsServerWithoutLoading() {
    val initialized =
      initLoginUiState(
        navKey = Login(),
        pendingOpenIdServer = "https://example.com/audiobookshelf",
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
      )

    assertEquals("https://example.com/audiobookshelf", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(ServerAccessMode.LocalNetwork, initialized.serverAccessMode)
    assertEquals(LoginDiscoveryState.Idle, initialized.discoveryState)
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
    assertEquals(LoginDiscoveryState.Idle, prepared.discoveryState)
    assertEquals(listOf(LoginMethod.Local), prepared.availableLoginMethods)
    assertNull(prepared.loginDiscoveryMessage)
    assertNull(prepared.authLoginCustomMessage)
    assertNull(prepared.authOpenIdButtonText)
    assertNull(prepared.authOpenIdAutoLaunch)
  }

  @Test
  fun prepareLoginDiscovery_whenServerHasNoTopLevelDomain_doesNotNormalizeForDiscovery() {
    val prepared = LoginUiState().prepareLoginDiscovery("example")

    assertEquals("example", prepared.server)
    assertNull(prepared.normalizedServer)
    assertEquals(LoginDiscoveryState.Idle, prepared.discoveryState)
  }

  @Test
  fun applyLoginDiscovery_whenBothMethodsExist_keepsPasswordLoginAndShowsOpenIdAlternative() {
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
  fun applyLoginDiscovery_whenBothMethodsExistOnHttpServer_hidesOpenIdAlternative() {
    val applied =
      LoginUiState(server = "http://example.com")
        .applyLoginDiscovery(
          LoginDiscoveryResult(
            normalizedServer = "http://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
            authOpenIdButtonText = "Continue with Acme SSO",
          )
        )

    assertEquals("Continue with Acme SSO", applied.authOpenIdButtonText)
    assertEquals(false, applied.showsMixedLoginMethods())
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
  fun applyLoginDiscovery_whenOnlyOpenIdExistsOnHttpServer_hidesOpenIdSurface() {
    val applied =
      LoginUiState(server = "http://example.com")
        .applyLoginDiscovery(
          LoginDiscoveryResult(
            normalizedServer = "http://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.OpenId),
            loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
            authOpenIdButtonText = "Continue with Acme SSO",
          )
        )

    assertEquals("Continue with Acme SSO", applied.authOpenIdButtonText)
    assertEquals(false, applied.isOpenIdOnly())
  }

  @Test
  fun applyLoginDiscovery_whenForcedReloginServerOnlySupportsOpenId_preservesReloginContext() {
    val applied =
      LoginUiState(
          server = "https://example.com",
          reLogin = true,
          authPromptReason = AuthPromptReason.RefreshFailed,
        )
        .applyLoginDiscovery(
          LoginDiscoveryResult(
            normalizedServer = "https://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.OpenId),
            loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
          )
        )

    assertTrue(applied.reLogin)
    assertEquals(AuthPromptReason.RefreshFailed, applied.authPromptReason)
    assertTrue(applied.isOpenIdOnly())
  }

  @Test
  fun prepareOpenIdRecovery_whenPendingCallbackMatchesCurrentServer_showsLoadingWithoutResettingDiscovery() {
    val prepared =
      LoginUiState(
          server = "https://example.com",
          normalizedServer = "https://example.com",
          discoveryState = LoginDiscoveryState.Success,
          availableLoginMethods = listOf(LoginMethod.OpenId),
          loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
        )
        .prepareOpenIdRecovery(
          OpenIdLoginRecoveryState(
            normalizedServer = "https://example.com",
            hasPendingCallback = true,
          )
        )

    assertEquals(GenericState.Loading, prepared.loginState)
    assertEquals(LoginDiscoveryState.Success, prepared.discoveryState)
    assertTrue(prepared.isOpenIdOnly())
  }

  @Test
  fun applyOpenIdRecoveryCompletion_whenFailureTargetsCurrentServer_keepsOpenIdSurfaceAndShowsFailure() {
    val applied =
      LoginUiState(
          server = "https://example.com",
          normalizedServer = "https://example.com",
          loginState = GenericState.Loading,
          discoveryState = LoginDiscoveryState.Success,
          availableLoginMethods = listOf(LoginMethod.OpenId),
          loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
        )
        .applyOpenIdRecoveryCompletion(
          OpenIdLoginCompletionResult.Failed(
            OpenIdLoginFailure(
              normalizedServer = "https://example.com",
              errorMessage = "OpenID login failed. Please try again.",
            )
          )
        )

    assertTrue(applied.loginState is GenericState.Failure)
    assertTrue(applied.isOpenIdOnly())
    assertEquals(
      "OpenID login failed. Please try again.",
      (applied.loginState as GenericState.Failure).errorMessage,
    )
  }

  @Test
  fun applyOpenIdRecoveryCompletion_whenSuccess_marksStateSuccessful() {
    val applied =
      LoginUiState(server = "https://example.com", loginState = GenericState.Loading)
        .applyOpenIdRecoveryCompletion(OpenIdLoginCompletionResult.Success)

    assertEquals(GenericState.Success, applied.loginState)
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
        redirectUri = "audiobookshelf://oauth",
        startOpenIdLogin = { uiState, _ ->
          OpenIdLoginStartResult(
            uiState =
              uiState.copy(
                server = "https://example.com",
                normalizedServer = "https://example.com",
              ),
            authorizationUrl =
              "https://example.com/auth/openid?redirect_uri=audiobookshelf://oauth",
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
      "https://example.com/auth/openid?redirect_uri=audiobookshelf://oauth",
      (event as LoginUiEvent.LaunchOpenIdLogin).authorizationUrl,
    )
  }
}
