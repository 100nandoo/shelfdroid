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
import dev.halim.shelfdroid.core.data.screen.login.LoginRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.navigation.Login
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
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
  private val settingsRepository: SettingsRepository,
  @Assisted private val navKey: Login,
) : ViewModel() {

  private val _uiState = MutableStateFlow(initUiState())

  val uiState: StateFlow<LoginUiState> = _uiState

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
      LoginEvent.UseDifferentServerOrAccountConfirmed ->
        viewModelScope.launch {
          settingsRepository.logout().onFailure { error ->
            _uiState.update { it.copy(loginState = GenericState.Failure(error.message)) }
          }
        }
      is LoginEvent.ServerChanged -> _uiState.update { it.prepareDiscovery(event.server) }
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
        .debounce(300)
        .distinctUntilChanged()
        .collectLatest { server ->
          val parsed = AudiobookshelfBaseUrl.parse(server)
          if (parsed == null) {
            _uiState.update { it.prepareDiscovery(server) }
            return@collectLatest
          }

          _uiState.update {
            it.copy(
              normalizedServer = parsed.value,
              discoveryState = LoginDiscoveryState.Loading,
            )
          }
          val result = loginRepository.discoverLoginMethods(server)
          _uiState.update { it.applyDiscovery(result) }
        }
    }
  }

  private fun initUiState(): LoginUiState {
    val state =
      if (navKey.reLogin) {
        runBlocking {
          val username = loginRepository.userPrefs.firstOrNull()?.username ?: ""
          val server = if (username.isNotBlank()) loginRepository.baseUrl else ""
          LoginUiState(
            username = username,
            server = server,
            reLogin = true,
            authPromptReason = navKey.reason,
          )
        }
      } else {
        LoginUiState(authPromptReason = navKey.reason)
      }
    return state.prepareDiscovery(state.server)
  }

  private fun LoginUiState.prepareDiscovery(server: String): LoginUiState {
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

  private fun LoginUiState.applyDiscovery(result: LoginDiscoveryResult): LoginUiState {
    return copy(
      normalizedServer = result.normalizedServer,
      discoveryState = result.discoveryState,
      availableLoginMethods = result.availableLoginMethods,
      loginDiscoveryMessage = result.loginDiscoveryMessage,
      authLoginCustomMessage = result.authLoginCustomMessage,
      authOpenIdButtonText = result.authOpenIdButtonText,
      authOpenIdAutoLaunch = result.authOpenIdAutoLaunch,
    )
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: Login): LoginViewModel
  }
}
