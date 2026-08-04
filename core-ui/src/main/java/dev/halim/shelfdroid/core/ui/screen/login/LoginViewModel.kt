package dev.halim.shelfdroid.core.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryResult
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginEvent
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.data.screen.login.OpenIdCallbackCoordinator
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailure
import dev.halim.shelfdroid.core.data.screen.login.LoginRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.navigation.Login
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
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
  private val openIdCallbackCoordinator: OpenIdCallbackCoordinator,
  private val settingsRepository: SettingsRepository,
  @Assisted private val navKey: Login,
) : ViewModel() {

  private val _uiState = MutableStateFlow(initUiState())

  val uiState: StateFlow<LoginUiState> = _uiState

  private val _events = MutableSharedFlow<LoginUiEvent>()
  val events = _events.asSharedFlow()

  init {
    observeLoginDiscovery()
  }

  fun onEvent(event: LoginEvent) {
    when (event) {
      is LoginEvent.LoginButtonPressed ->
        viewModelScope.launch {
          val currentState = _uiState.value
          _uiState.update { it.copy(loginState = GenericState.Loading) }
          _uiState.update { loginRepository.login(currentState) }
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
          settingsRepository.logout().onFailure { error ->
            _uiState.update { it.copy(loginState = GenericState.Failure(error.message)) }
          }
        }

      is LoginEvent.ServerChanged -> _uiState.update { it.prepareLoginDiscovery(event.server) }
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
        .map { it.server }
        .debounce(500.milliseconds)
        .distinctUntilChanged()
        .collectLatest { server ->
          val parsed = AudiobookshelfBaseUrl.parse(server)
          if (parsed == null) {
            _uiState.update { it.prepareLoginDiscovery(server) }
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

  private fun initUiState(): LoginUiState {
    val openIdLoginFailure = runBlocking { openIdCallbackCoordinator.consumeFailure() }
    val (username, server) =
      if (navKey.reLogin) {
        runBlocking {
          val username = loginRepository.userPrefs.firstOrNull()?.username ?: ""
          val server = if (username.isNotBlank()) loginRepository.baseUrl else ""
          username to server
        }
      } else {
        "" to ""
      }
    return initLoginUiState(
      navKey = navKey,
      username = username,
      server = server,
      openIdLoginFailure = openIdLoginFailure,
    )
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: Login): LoginViewModel
  }
}

sealed interface LoginUiEvent {
  data class LaunchOpenIdLogin(val authorizationUrl: String) : LoginUiEvent
}

internal suspend fun handleOpenIdLoginButtonPressed(
  uiState: LoginUiState,
  redirectUri: String,
  startOpenIdLogin: suspend (LoginUiState, String) -> dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginStartResult,
  emitEvent: suspend (LoginUiEvent) -> Unit,
): LoginUiState {
  val result = startOpenIdLogin(uiState, redirectUri)
  result.authorizationUrl?.let { emitEvent(LoginUiEvent.LaunchOpenIdLogin(it)) }
  return result.uiState
}

internal fun LoginUiState.prepareLoginDiscovery(server: String): LoginUiState {
  val normalizedServer = AudiobookshelfBaseUrl.parse(server)?.value
  return copy(
    server = server,
    normalizedServer = normalizedServer,
    serverFieldError = null,
    discoveryState =
      if (normalizedServer != null) LoginDiscoveryState.Loading else LoginDiscoveryState.Idle,
    availableLoginMethods = listOf(LoginMethod.Local),
    loginDiscoveryMessage = null,
    authLoginCustomMessage = null,
    authOpenIdButtonText = null,
    authOpenIdAutoLaunch = null,
  )
}

internal fun initLoginUiState(
  navKey: Login,
  username: String = "",
  server: String = "",
  openIdLoginFailure: OpenIdLoginFailure? = null,
): LoginUiState {
  val initialServer = if (navKey.reLogin) server else openIdLoginFailure?.normalizedServer.orEmpty()
  val state =
    if (navKey.reLogin) {
      LoginUiState(
        username = username,
        server = if (username.isNotBlank()) initialServer else "",
        reLogin = true,
        authPromptReason = navKey.reason,
        loginState =
          openIdLoginFailure?.let { GenericState.Failure(it.errorMessage) } ?: GenericState.Idle,
      )
    } else {
      LoginUiState(
        server = initialServer,
        authPromptReason = navKey.reason,
        loginState =
          openIdLoginFailure?.let { GenericState.Failure(it.errorMessage) } ?: GenericState.Idle,
      )
    }
  return state.prepareLoginDiscovery(state.server)
}

internal fun LoginUiState.applyLoginDiscovery(result: LoginDiscoveryResult): LoginUiState {
  val availableLoginMethods = result.availableLoginMethods.ifEmpty { listOf(LoginMethod.Local) }
  return copy(
    normalizedServer = result.normalizedServer,
    discoveryState = result.discoveryState,
    availableLoginMethods = availableLoginMethods,
    loginDiscoveryMessage = result.loginDiscoveryMessage,
    authLoginCustomMessage = result.authLoginCustomMessage,
    authOpenIdButtonText = result.authOpenIdButtonText,
    authOpenIdAutoLaunch = result.authOpenIdAutoLaunch,
  )
}
