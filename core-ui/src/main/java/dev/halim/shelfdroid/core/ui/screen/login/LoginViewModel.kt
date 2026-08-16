package dev.halim.shelfdroid.core.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LocalNetworkPermissionState
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryMessage
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryResult
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginEvent
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.data.screen.login.LoginRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginCompletionResult
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailure
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailureStore
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginRecoveryState
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginStartResult
import dev.halim.shelfdroid.core.data.screen.login.PendingLocalNetworkAction
import dev.halim.shelfdroid.core.data.sessionreset.SessionResetRepository
import dev.halim.shelfdroid.core.ui.navigation.Login
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(FlowPreview::class)
@HiltViewModel(assistedFactory = LoginViewModel.Factory::class)
class LoginViewModel
@AssistedInject
constructor(
  private val loginRepository: LoginRepository,
  private val openIdLoginFailureStore: OpenIdLoginFailureStore,
  private val sessionResetRepository: SessionResetRepository,
  @Assisted private val navKey: Login,
) : ViewModel() {

  private val _uiState = MutableStateFlow(initUiState())

  val uiState: StateFlow<LoginUiState> = _uiState

  private val _events = MutableSharedFlow<LoginUiEvent>()
  val events = _events.asSharedFlow()

  init {
    observeLoginDiscovery()
    recoverPendingOpenIdLogin()
  }

  fun onEvent(event: LoginEvent) {
    when (event) {
      is LoginEvent.LoginButtonPressed ->
        viewModelScope.launch {
          val currentState = _uiState.value
          val hasValidServer =
            currentState.normalizedServer != null ||
              AudiobookshelfBaseUrl.parse(currentState.server) != null
          if (currentState.serverAccessMode == ServerAccessMode.LocalNetwork && hasValidServer) {
            requestLocalNetworkPermission(PendingLocalNetworkAction.PasswordLogin)
            return@launch
          }

          val loadingState = currentState.copy(loginState = GenericState.Loading)
          _uiState.value = loadingState
          _uiState.value = loginRepository.login(loadingState)
        }

      is LoginEvent.OpenIdLoginButtonPressed ->
        viewModelScope.launch {
          _uiState.value =
            handleOpenIdLoginButtonPressed(
              uiState = _uiState.value,
              redirectUri = event.redirectUri,
              startOpenIdLogin = loginRepository::startOpenIdLogin,
              emitEvent = _events::emit,
            )
        }

      LoginEvent.UseDifferentServerOrAccountConfirmed ->
        viewModelScope.launch {
          val errorMessage =
            handleAccountSwitch(
              resetSessionForAccountSwitch = sessionResetRepository::logoutForAccountSwitch,
              emitEvent = _events::emit,
            )
          errorMessage?.let { error ->
            _uiState.update { it.copy(loginState = GenericState.Failure(error)) }
          }
        }

      is LoginEvent.ServerChanged -> _uiState.update { it.prepareLoginDiscovery(event.server) }
      is LoginEvent.LocalNetworkPermissionResult ->
        viewModelScope.launch {
          _uiState.value =
            handleLocalNetworkPermissionResult(
              uiState = _uiState.value,
              granted = event.granted,
              permanentlyDenied = event.permanentlyDenied,
              login = loginRepository::login,
              discoverLoginMethods = loginRepository::discoverLoginMethods,
              startOpenIdLogin = loginRepository::startOpenIdLogin,
              completeOpenIdLogin = loginRepository::completeOpenIdLogin,
              emitEvent = _events::emit,
            )
        }
      is LoginEvent.ServerAccessModeChanged ->
        _uiState.update {
          it.prepareLoginDiscovery(
            server = it.server,
            serverAccessMode = event.serverAccessMode,
          )
        }
      is LoginEvent.UsernameChanged -> _uiState.update { it.copy(username = event.username) }
      is LoginEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
      LoginEvent.ErrorShown -> {
        _uiState.update { it.copy(loginState = GenericState.Idle) }
      }
    }
  }

  private fun observeLoginDiscovery() {
    viewModelScope.launch {
      uiState
        .map { it.server to it.serverAccessMode }
        .debounce(500.milliseconds)
        .distinctUntilChanged()
        .collectLatest { (server, serverAccessMode) ->
          val parsed = AudiobookshelfBaseUrl.parse(server)
          if (parsed == null || parsed.isNotReadyForDiscovery()) {
            _uiState.update { it.prepareLoginDiscovery(server) }
            return@collectLatest
          }

          if (serverAccessMode == ServerAccessMode.LocalNetwork) {
            requestLocalNetworkPermission(PendingLocalNetworkAction.DiscoverLoginMethods)
            return@collectLatest
          }

          _uiState.update {
            it.copy(
              normalizedServer = parsed.value,
              discoveryState = LoginDiscoveryState.Loading,
            )
          }
          val result = loginRepository.discoverLoginMethods(server)
          _uiState.update { it.applyLoginDiscovery(result) }
        }
    }
  }

  private suspend fun requestLocalNetworkPermission(action: PendingLocalNetworkAction) {
    _uiState.update { it.prepareLocalNetworkPermissionRequest(action) }
    _events.emit(LoginUiEvent.RequestLocalNetworkPermission)
  }

  private fun initUiState(): LoginUiState {
    val openIdLoginRecoveryState = runBlocking { loginRepository.openIdLoginRecoveryState() }
    val openIdLoginFailure = runBlocking { openIdLoginFailureStore.consume() }
    val savedServer = loginRepository.baseUrl
    val savedServerAccessMode =
      when {
        openIdLoginFailure != null -> openIdLoginFailure.serverAccessMode
        openIdLoginRecoveryState.normalizedServer != null ->
          openIdLoginRecoveryState.serverAccessMode
        else -> runBlocking { loginRepository.currentServerAccessMode() }
      }
    val savedServerForAccessMode =
      when {
        openIdLoginFailure != null -> openIdLoginFailure.normalizedServer
        openIdLoginRecoveryState.normalizedServer != null ->
          openIdLoginRecoveryState.normalizedServer
        else -> savedServer
      }
    val (username, server) =
      if (navKey.reLogin) {
        runBlocking {
          val username = loginRepository.userPrefs.firstOrNull()?.username ?: ""
          val server = if (username.isNotBlank()) savedServer else ""
          username to server
        }
      } else {
        "" to savedServer
      }
    return initLoginUiState(
      navKey = navKey,
      username = username,
      server = server,
      openIdLoginFailure = openIdLoginFailure,
      pendingOpenIdServer = openIdLoginRecoveryState.normalizedServer,
      savedServerAccessMode = savedServerAccessMode,
      savedServerForAccessMode = savedServerForAccessMode,
    )
  }

  private fun recoverPendingOpenIdLogin() {
    viewModelScope.launch {
      val recoveryState = loginRepository.openIdLoginRecoveryState()
      _uiState.value =
        handlePendingOpenIdRecovery(
          uiState = _uiState.value,
          recoveryState = recoveryState,
          completeOpenIdLogin = loginRepository::completeOpenIdLogin,
          emitEvent = _events::emit,
        )
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: Login): LoginViewModel
  }
}

sealed interface LoginUiEvent {
  data class LaunchOpenIdLogin(val authorizationUrl: String) : LoginUiEvent

  data object RequestLocalNetworkPermission : LoginUiEvent

  data object LoggedOut : LoginUiEvent
}

internal suspend fun handleAccountSwitch(
  resetSessionForAccountSwitch: suspend () -> Result<Unit>,
  emitEvent: suspend (LoginUiEvent) -> Unit,
): String? {
  val result = resetSessionForAccountSwitch()
  result.exceptionOrNull()?.let { error ->
    return error.message
  }

  emitEvent(LoginUiEvent.LoggedOut)
  return null
}

internal suspend fun handleOpenIdLoginButtonPressed(
  uiState: LoginUiState,
  redirectUri: String,
  startOpenIdLogin:
    suspend (
      LoginUiState,
      String,
    ) -> OpenIdLoginStartResult,
  emitEvent: suspend (LoginUiEvent) -> Unit,
): LoginUiState {
  if (uiState.requiresLocalNetworkPermission() && uiState.hasValidServer()) {
    emitEvent(LoginUiEvent.RequestLocalNetworkPermission)
    return uiState.prepareLocalNetworkPermissionRequest(
      PendingLocalNetworkAction.OpenIdLoginStart(redirectUri)
    )
  }

  return launchOpenIdLogin(
    uiState = uiState,
    redirectUri = redirectUri,
    startOpenIdLogin = startOpenIdLogin,
    emitEvent = emitEvent,
  )
}

private suspend fun launchOpenIdLogin(
  uiState: LoginUiState,
  redirectUri: String,
  startOpenIdLogin:
    suspend (
      LoginUiState,
      String,
    ) -> OpenIdLoginStartResult,
  emitEvent: suspend (LoginUiEvent) -> Unit,
): LoginUiState {
  val result = startOpenIdLogin(uiState, redirectUri)
  result.authorizationUrl?.let { emitEvent(LoginUiEvent.LaunchOpenIdLogin(it)) }
  return result.uiState
}

internal fun LoginUiState.prepareLoginDiscovery(server: String): LoginUiState {
  return prepareLoginDiscovery(server = server, serverAccessMode = serverAccessMode)
}

internal fun LoginUiState.prepareLoginDiscovery(
  server: String,
  serverAccessMode: ServerAccessMode,
): LoginUiState {
  val parsedServer = AudiobookshelfBaseUrl.parse(server)
  val normalizedServer = parsedServer?.takeUnless { it.isNotReadyForDiscovery() }?.value
  return copy(
    server = server,
    normalizedServer = normalizedServer,
    serverFieldError = null,
    serverAccessMode = serverAccessMode,
    pendingLocalNetworkAction = null,
    localNetworkPermissionState = null,
    discoveryState = LoginDiscoveryState.Idle,
    availableLoginMethods = listOf(LoginMethod.Local),
    loginDiscoveryMessage = null,
    authLoginCustomMessage = null,
    authOpenIdButtonText = null,
    authOpenIdAutoLaunch = null,
  )
}

private fun AudiobookshelfBaseUrl.isNotReadyForDiscovery(): Boolean {
  val hasTopLevelDomain = host.substringAfterLast('.', missingDelimiterValue = "").isNotBlank()
  val hasLocalServerHint = host == "localhost" || port != -1
  return !hasTopLevelDomain && !hasLocalServerHint
}

internal fun LoginUiState.prepareLocalNetworkPermissionRequest(
  action: PendingLocalNetworkAction
): LoginUiState {
  return copy(
    pendingLocalNetworkAction = action,
    localNetworkPermissionState = null,
  )
}

private fun LoginUiState.recordLocalNetworkPermissionDenial(
  permanentlyDenied: Boolean
): LoginUiState {
  return copy(
    loginState = GenericState.Idle,
    pendingLocalNetworkAction = null,
    localNetworkPermissionState =
      if (permanentlyDenied) {
        LocalNetworkPermissionState.PermanentlyDenied
      } else {
        LocalNetworkPermissionState.Denied
      },
  )
}

private fun LoginUiState.clearLocalNetworkPermissionState(): LoginUiState {
  return copy(
    pendingLocalNetworkAction = null,
    localNetworkPermissionState = null,
  )
}

internal suspend fun handleLocalNetworkPermissionResult(
  uiState: LoginUiState,
  granted: Boolean,
  permanentlyDenied: Boolean,
  login: suspend (LoginUiState) -> LoginUiState,
  discoverLoginMethods: suspend (String) -> LoginDiscoveryResult,
  startOpenIdLogin: suspend (LoginUiState, String) -> OpenIdLoginStartResult = { _, _ ->
    error("OpenID login start should not run for this action")
  },
  completeOpenIdLogin: suspend () -> OpenIdLoginCompletionResult = {
    error("OpenID login completion should not run for this action")
  },
  emitEvent: suspend (LoginUiEvent) -> Unit = {},
): LoginUiState {
  val pendingAction = uiState.pendingLocalNetworkAction ?: return uiState
  if (!granted) {
    return uiState.recordLocalNetworkPermissionDenial(permanentlyDenied)
  }

  val clearedState = uiState.clearLocalNetworkPermissionState()
  return when (pendingAction) {
    PendingLocalNetworkAction.DiscoverLoginMethods -> {
      val parsedServer = AudiobookshelfBaseUrl.parse(clearedState.server)
      if (parsedServer == null || parsedServer.isNotReadyForDiscovery()) {
        clearedState.prepareLoginDiscovery(clearedState.server)
      } else {
        val loadingState =
          clearedState.copy(
            normalizedServer = parsedServer.value,
            discoveryState = LoginDiscoveryState.Loading,
          )
        val result = discoverLoginMethods(clearedState.server)
        loadingState.applyLoginDiscovery(result)
      }
    }

    PendingLocalNetworkAction.PasswordLogin -> {
      val loadingState = clearedState.copy(loginState = GenericState.Loading)
      login(loadingState)
    }

    is PendingLocalNetworkAction.OpenIdLoginStart ->
      launchOpenIdLogin(
        uiState = clearedState,
        redirectUri = pendingAction.redirectUri,
        startOpenIdLogin = startOpenIdLogin,
        emitEvent = emitEvent,
      )

    PendingLocalNetworkAction.CompleteOpenIdLogin -> {
      val result = completeOpenIdLogin()
      clearedState.applyOpenIdRecoveryCompletion(result)
    }
  }
}

internal fun initLoginUiState(
  navKey: Login,
  username: String = "",
  server: String = "",
  openIdLoginFailure: OpenIdLoginFailure? = null,
  pendingOpenIdServer: String? = null,
  savedServerAccessMode: ServerAccessMode = ServerAccessMode.Internet,
  savedServerForAccessMode: String? = null,
): LoginUiState {
  val initialServer =
    when {
      navKey.reLogin -> server
      !openIdLoginFailure?.normalizedServer.isNullOrBlank() ->
        openIdLoginFailure.normalizedServer.orEmpty()
      !pendingOpenIdServer.isNullOrBlank() -> pendingOpenIdServer.orEmpty()
      server.isNotBlank() -> server
      else -> ""
    }
  val normalizedInitialServer = AudiobookshelfBaseUrl.parse(initialServer)?.value ?: initialServer
  val normalizedSavedServer =
    savedServerForAccessMode?.let { AudiobookshelfBaseUrl.parse(it)?.value ?: it }
      ?: normalizedInitialServer.takeIf { it.isNotBlank() }
  val initialServerAccessMode =
    if (normalizedInitialServer.isNotBlank() && normalizedInitialServer == normalizedSavedServer) {
      savedServerAccessMode
    } else {
      ServerAccessMode.Internet
    }
  val state =
    if (navKey.reLogin) {
      LoginUiState(
        username = username,
        server = if (username.isNotBlank()) initialServer else "",
        serverAccessMode = initialServerAccessMode,
        reLogin = true,
        authPromptReason = navKey.reason,
        loginState =
          openIdLoginFailure?.let { GenericState.Failure(it.errorMessage) } ?: GenericState.Idle,
      )
    } else {
      LoginUiState(
        server = initialServer,
        serverAccessMode = initialServerAccessMode,
        authPromptReason = navKey.reason,
        loginState =
          openIdLoginFailure?.let { GenericState.Failure(it.errorMessage) } ?: GenericState.Idle,
      )
    }
  return state.prepareLoginDiscovery(state.server)
}

internal fun LoginUiState.prepareOpenIdRecovery(
  recoveryState: OpenIdLoginRecoveryState
): LoginUiState {
  val prepared = reconcileOpenIdServer(recoveryState.normalizedServer)
  return prepared.copy(
    loginState = GenericState.Loading,
    serverFieldError = null,
    serverAccessMode = recoveryState.serverAccessMode,
  )
}

internal suspend fun handlePendingOpenIdRecovery(
  uiState: LoginUiState,
  recoveryState: OpenIdLoginRecoveryState,
  completeOpenIdLogin: suspend () -> OpenIdLoginCompletionResult,
  emitEvent: suspend (LoginUiEvent) -> Unit,
): LoginUiState {
  if (!recoveryState.hasPendingCallback) return uiState

  val prepared = uiState.prepareOpenIdRecovery(recoveryState)
  if (prepared.requiresLocalNetworkPermission()) {
    emitEvent(LoginUiEvent.RequestLocalNetworkPermission)
    return prepared.prepareLocalNetworkPermissionRequest(
      PendingLocalNetworkAction.CompleteOpenIdLogin
    )
  }

  val result = completeOpenIdLogin()
  return prepared.applyOpenIdRecoveryCompletion(result)
}

internal fun LoginUiState.applyOpenIdRecoveryCompletion(
  result: OpenIdLoginCompletionResult
): LoginUiState {
  return when (result) {
    OpenIdLoginCompletionResult.Success -> copy(loginState = GenericState.Success)
    is OpenIdLoginCompletionResult.Failed -> {
      val prepared = reconcileOpenIdServer(result.failure.normalizedServer)
      prepared.copy(
        loginState = GenericState.Failure(result.failure.errorMessage),
        serverFieldError = null,
      )
    }
  }
}

private fun LoginUiState.reconcileOpenIdServer(candidateServer: String?): LoginUiState {
  val server = candidateServer?.takeIf { it.isNotBlank() } ?: return this
  return if (this.server == server) {
    copy(
      server = server,
      normalizedServer = AudiobookshelfBaseUrl.parse(server)?.value ?: normalizedServer,
    )
  } else {
    prepareLoginDiscovery(server)
  }
}

internal fun LoginUiState.applyLoginDiscovery(result: LoginDiscoveryResult): LoginUiState {
  val availableLoginMethods = result.availableLoginMethods.ifEmpty { listOf(LoginMethod.Local) }
  val loginDiscoveryMessage =
    if (
      result.discoveryState is LoginDiscoveryState.Failure &&
        serverAccessMode == ServerAccessMode.Internet &&
        result.loginDiscoveryMessage == LoginDiscoveryMessage.MethodsUnconfirmed
    ) {
      LoginDiscoveryMessage.MethodsUnconfirmedTryLocalNetwork
    } else {
      result.loginDiscoveryMessage
    }
  return copy(
    normalizedServer = result.normalizedServer,
    discoveryState = result.discoveryState,
    availableLoginMethods = availableLoginMethods,
    loginDiscoveryMessage = loginDiscoveryMessage,
    authLoginCustomMessage = result.authLoginCustomMessage,
    authOpenIdButtonText = result.authOpenIdButtonText,
    authOpenIdAutoLaunch = result.authOpenIdAutoLaunch,
  )
}

private fun LoginUiState.requiresLocalNetworkPermission(): Boolean {
  return serverAccessMode == ServerAccessMode.LocalNetwork
}

private fun LoginUiState.hasValidServer(): Boolean {
  return normalizedServer != null || AudiobookshelfBaseUrl.parse(server) != null
}
