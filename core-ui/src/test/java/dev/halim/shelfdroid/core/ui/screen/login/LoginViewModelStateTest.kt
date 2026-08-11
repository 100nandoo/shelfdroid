package dev.halim.shelfdroid.core.ui.screen.login

import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LocalNetworkPermissionState
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryMessage
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryResult
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginCompletionResult
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailure
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginRecoveryState
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginStartResult
import dev.halim.shelfdroid.core.data.screen.login.PendingLocalNetworkAction
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
  fun initLoginUiState_whenSavedServerExists_restoresSavedServerAndAccessMode() {
    val initialized =
      initLoginUiState(
        navKey = Login(),
        server = "https://example.com/audiobookshelf/",
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
        savedServerForAccessMode = "https://example.com/audiobookshelf",
      )

    assertEquals("https://example.com/audiobookshelf/", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(ServerAccessMode.LocalNetwork, initialized.serverAccessMode)
  }

  @Test
  fun initLoginUiState_whenForcedReloginHasSavedServer_preparesDiscoveryWithoutLoading() {
    val initialized =
      initLoginUiState(
        navKey = Login(reLogin = true, reason = AuthPromptReason.RefreshFailed),
        username = "fernando",
        server = "https://example.com/audiobookshelf/",
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
        savedServerForAccessMode = "https://example.com/audiobookshelf",
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
        savedServerForAccessMode = "https://example.com/audiobookshelf",
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
        savedServerForAccessMode = "https://example.com/audiobookshelf",
      )

    assertEquals("https://example.com/audiobookshelf", initialized.server)
    assertEquals("https://example.com/audiobookshelf", initialized.normalizedServer)
    assertEquals(ServerAccessMode.LocalNetwork, initialized.serverAccessMode)
    assertEquals(LoginDiscoveryState.Idle, initialized.discoveryState)
  }

  @Test
  fun initLoginUiState_whenSavedAccessModeTargetsDifferentServer_defaultsToInternet() {
    val initialized =
      initLoginUiState(
        navKey = Login(),
        server = "https://other.example.com",
        savedServerAccessMode = ServerAccessMode.LocalNetwork,
        savedServerForAccessMode = "https://example.com",
      )

    assertEquals(ServerAccessMode.Internet, initialized.serverAccessMode)
  }

  @Test
  fun prepareLoginDiscovery_whenServerChanges_clearsStaleDiscoveryState() {
    val prepared =
      LoginUiState(
          server = "https://old.example.com",
          normalizedServer = "https://old.example.com",
          serverAccessMode = ServerAccessMode.LocalNetwork,
          pendingLocalNetworkAction = PendingLocalNetworkAction.DiscoverLoginMethods,
          localNetworkPermissionState = LocalNetworkPermissionState.PermanentlyDenied,
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
    assertEquals(ServerAccessMode.LocalNetwork, prepared.serverAccessMode)
    assertEquals(LoginDiscoveryState.Idle, prepared.discoveryState)
    assertEquals(listOf(LoginMethod.Local), prepared.availableLoginMethods)
    assertNull(prepared.loginDiscoveryMessage)
    assertNull(prepared.authLoginCustomMessage)
    assertNull(prepared.authOpenIdButtonText)
    assertNull(prepared.authOpenIdAutoLaunch)
    assertNull(prepared.pendingLocalNetworkAction)
    assertNull(prepared.localNetworkPermissionState)
  }

  @Test
  fun prepareLoginDiscovery_whenServerHasNoTopLevelDomain_doesNotNormalizeForDiscovery() {
    val prepared = LoginUiState().prepareLoginDiscovery("example")

    assertEquals("example", prepared.server)
    assertNull(prepared.normalizedServer)
    assertEquals(LoginDiscoveryState.Idle, prepared.discoveryState)
  }

  @Test
  fun prepareLocalNetworkPermissionRequest_tracksPendingActionAndClearsPriorGuidance() {
    val prepared =
      LoginUiState(localNetworkPermissionState = LocalNetworkPermissionState.PermanentlyDenied)
        .prepareLocalNetworkPermissionRequest(PendingLocalNetworkAction.PasswordLogin)

    assertEquals(PendingLocalNetworkAction.PasswordLogin, prepared.pendingLocalNetworkAction)
    assertNull(prepared.localNetworkPermissionState)
  }

  @Test
  fun handleLocalNetworkPermissionResult_whenDeniedOnce_recordsRecoverableGuidance() = runBlocking {
    val updated =
      handleLocalNetworkPermissionResult(
        uiState =
          LoginUiState(serverAccessMode = ServerAccessMode.LocalNetwork)
            .prepareLocalNetworkPermissionRequest(PendingLocalNetworkAction.PasswordLogin),
        granted = false,
        permanentlyDenied = false,
        login = { throw AssertionError("login should not run when permission is denied") },
        discoverLoginMethods = {
          throw AssertionError("discovery should not run when permission is denied")
        },
      )

    assertEquals(LocalNetworkPermissionState.Denied, updated.localNetworkPermissionState)
    assertNull(updated.pendingLocalNetworkAction)
  }

  @Test
  fun handleLocalNetworkPermissionResult_whenPermanentlyDenied_recordsSettingsGuidance() =
    runBlocking {
      val updated =
        handleLocalNetworkPermissionResult(
          uiState =
            LoginUiState(serverAccessMode = ServerAccessMode.LocalNetwork)
              .prepareLocalNetworkPermissionRequest(PendingLocalNetworkAction.DiscoverLoginMethods),
          granted = false,
          permanentlyDenied = true,
          login = { throw AssertionError("login should not run when permission is denied") },
          discoverLoginMethods = {
            throw AssertionError("discovery should not run when permission is denied")
          },
        )

      assertEquals(
        LocalNetworkPermissionState.PermanentlyDenied,
        updated.localNetworkPermissionState,
      )
      assertNull(updated.pendingLocalNetworkAction)
    }

  @Test
  fun handleLocalNetworkPermissionResult_whenGranted_resumesPendingDiscovery() = runBlocking {
    var discoveredServer: String? = null

    val updated =
      handleLocalNetworkPermissionResult(
        uiState =
          LoginUiState(
              server = "https://example.com",
              serverAccessMode = ServerAccessMode.LocalNetwork,
            )
            .prepareLocalNetworkPermissionRequest(PendingLocalNetworkAction.DiscoverLoginMethods),
        granted = true,
        permanentlyDenied = false,
        login = { throw AssertionError("login should not run for a discovery action") },
        discoverLoginMethods = { server ->
          discoveredServer = server
          LoginDiscoveryResult(
            normalizedServer = "https://example.com",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.OpenId),
            loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
          )
        },
      )

    assertEquals("https://example.com", discoveredServer)
    assertEquals(LoginDiscoveryState.Success, updated.discoveryState)
    assertEquals(listOf(LoginMethod.OpenId), updated.availableLoginMethods)
    assertEquals(LoginDiscoveryMessage.LocalLoginUnavailable, updated.loginDiscoveryMessage)
    assertNull(updated.pendingLocalNetworkAction)
    assertNull(updated.localNetworkPermissionState)
  }

  @Test
  fun handleLocalNetworkPermissionResult_whenGranted_resumesPendingPasswordLogin() = runBlocking {
    var loginRequestState: LoginUiState? = null

    val updated =
      handleLocalNetworkPermissionResult(
        uiState =
          LoginUiState(
              server = "https://example.com",
              normalizedServer = "https://example.com",
              serverAccessMode = ServerAccessMode.LocalNetwork,
              username = "fernando",
              password = "secret",
            )
            .prepareLocalNetworkPermissionRequest(PendingLocalNetworkAction.PasswordLogin),
        granted = true,
        permanentlyDenied = false,
        login = { state ->
          loginRequestState = state
          state.copy(loginState = GenericState.Success)
        },
        discoverLoginMethods = {
          throw AssertionError("discovery should not run for a password login action")
        },
      )

    assertEquals(GenericState.Loading, loginRequestState?.loginState)
    assertNull(loginRequestState?.pendingLocalNetworkAction)
    assertNull(loginRequestState?.localNetworkPermissionState)
    assertEquals(GenericState.Success, updated.loginState)
  }

  @Test
  fun handleLocalNetworkPermissionResult_whenGranted_resumesPendingOpenIdLoginStart() =
    runBlocking {
      val events = mutableListOf<LoginUiEvent>()
      var loginStartRequest: Pair<LoginUiState, String>? = null

      val updated =
        handleLocalNetworkPermissionResult(
          uiState =
            LoginUiState(
                server = "https://example.com",
                normalizedServer = "https://example.com",
                serverAccessMode = ServerAccessMode.LocalNetwork,
                discoveryState = LoginDiscoveryState.Success,
                availableLoginMethods = listOf(LoginMethod.OpenId),
              )
              .prepareLocalNetworkPermissionRequest(
                PendingLocalNetworkAction.OpenIdLoginStart("audiobookshelf://oauth")
              ),
          granted = true,
          permanentlyDenied = false,
          login = { throw AssertionError("password login should not run for OpenID start") },
          discoverLoginMethods = {
            throw AssertionError("discovery should not run for OpenID start")
          },
          startOpenIdLogin = { state, redirectUri ->
            loginStartRequest = state to redirectUri
            OpenIdLoginStartResult(
              uiState = state,
              authorizationUrl = "https://example.com/auth/openid?redirect_uri=$redirectUri",
            )
          },
          completeOpenIdLogin = {
            throw AssertionError("callback completion should not run for OpenID start")
          },
          emitEvent = { events += it },
        )

      assertEquals("audiobookshelf://oauth", loginStartRequest?.second)
      assertNull(loginStartRequest?.first?.pendingLocalNetworkAction)
      assertNull(loginStartRequest?.first?.localNetworkPermissionState)
      assertEquals(updated, loginStartRequest?.first)
      assertEquals(1, events.size)
      val event = events.single()
      assertTrue(event is LoginUiEvent.LaunchOpenIdLogin)
      assertEquals(
        "https://example.com/auth/openid?redirect_uri=audiobookshelf://oauth",
        (event as LoginUiEvent.LaunchOpenIdLogin).authorizationUrl,
      )
    }

  @Test
  fun handleLocalNetworkPermissionResult_whenGranted_resumesPendingOpenIdRecoveryCompletion() =
    runBlocking {
      var callbackCompletionRequested = false

      val updated =
        handleLocalNetworkPermissionResult(
          uiState =
            LoginUiState(
                server = "https://example.com",
                normalizedServer = "https://example.com",
                serverAccessMode = ServerAccessMode.LocalNetwork,
                loginState = GenericState.Loading,
                discoveryState = LoginDiscoveryState.Success,
                availableLoginMethods = listOf(LoginMethod.OpenId),
                loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
              )
              .prepareLocalNetworkPermissionRequest(
                PendingLocalNetworkAction.CompleteOpenIdLogin
              ),
          granted = true,
          permanentlyDenied = false,
          login = { throw AssertionError("password login should not run for OpenID recovery") },
          discoverLoginMethods = {
            throw AssertionError("discovery should not run for OpenID recovery")
          },
          startOpenIdLogin = { _, _ ->
            throw AssertionError("OpenID start should not run for OpenID recovery")
          },
          completeOpenIdLogin = {
            callbackCompletionRequested = true
            OpenIdLoginCompletionResult.Success
          },
          emitEvent = { throw AssertionError("no event should be emitted after permission grant") },
        )

      assertTrue(callbackCompletionRequested)
      assertEquals(GenericState.Success, updated.loginState)
      assertNull(updated.pendingLocalNetworkAction)
      assertNull(updated.localNetworkPermissionState)
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
  fun applyLoginDiscovery_whenFailureHappensInInternetMode_suggestsLocalNetwork() {
    val applied =
      LoginUiState(
          server = "https://example.com",
          serverAccessMode = ServerAccessMode.Internet,
        )
        .applyLoginDiscovery(
          LoginDiscoveryResult(
            normalizedServer = "https://example.com",
            discoveryState = LoginDiscoveryState.Failure,
            loginDiscoveryMessage = LoginDiscoveryMessage.MethodsUnconfirmed,
          )
        )

    assertEquals(
      LoginDiscoveryMessage.MethodsUnconfirmedTryLocalNetwork,
      applied.loginDiscoveryMessage,
    )
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
  fun handlePendingOpenIdRecovery_whenLocalNetworkAccessSelected_requestsPermissionAndDefersCompletion() =
    runBlocking {
      val events = mutableListOf<LoginUiEvent>()

      val updated =
        handlePendingOpenIdRecovery(
          uiState =
            LoginUiState(
              server = "https://example.com",
              normalizedServer = "https://example.com",
              discoveryState = LoginDiscoveryState.Success,
              availableLoginMethods = listOf(LoginMethod.OpenId),
              loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
            ),
          recoveryState =
            OpenIdLoginRecoveryState(
              normalizedServer = "https://example.com",
              serverAccessMode = ServerAccessMode.LocalNetwork,
              hasPendingCallback = true,
            ),
          completeOpenIdLogin = {
            throw AssertionError("callback completion should wait for permission grant")
          },
          emitEvent = { events += it },
        )

      assertEquals(GenericState.Loading, updated.loginState)
      assertEquals(
        PendingLocalNetworkAction.CompleteOpenIdLogin,
        updated.pendingLocalNetworkAction,
      )
      assertNull(updated.localNetworkPermissionState)
      assertEquals(1, events.size)
      assertEquals(LoginUiEvent.RequestLocalNetworkPermission, events.single())
    }

  @Test
  fun handleOpenIdLoginButtonPressed_whenLocalNetworkSelected_requestsPermissionInsteadOfStartingLogin() =
    runBlocking {
      val events = mutableListOf<LoginUiEvent>()
      var loginStartRequested = false

      val updated =
        handleOpenIdLoginButtonPressed(
          uiState =
            LoginUiState(
              server = "https://example.com",
              normalizedServer = "https://example.com",
              serverAccessMode = ServerAccessMode.LocalNetwork,
              discoveryState = LoginDiscoveryState.Success,
              availableLoginMethods = listOf(LoginMethod.OpenId),
            ),
          redirectUri = "audiobookshelf://oauth",
          startOpenIdLogin = { _, _ ->
            loginStartRequested = true
            throw AssertionError("OpenID login should wait for permission grant")
          },
          emitEvent = { events += it },
        )

      assertEquals(false, loginStartRequested)
      assertEquals(
        PendingLocalNetworkAction.OpenIdLoginStart("audiobookshelf://oauth"),
        updated.pendingLocalNetworkAction,
      )
      assertNull(updated.localNetworkPermissionState)
      assertEquals(1, events.size)
      assertEquals(LoginUiEvent.RequestLocalNetworkPermission, events.single())
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
