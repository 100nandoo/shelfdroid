package dev.halim.shelfdroid.core.ui.screen.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LoginEvent
import dev.halim.shelfdroid.core.data.screen.login.LoginRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltViewModel
class LoginViewModel
@Inject
constructor(private val loginRepository: LoginRepository, savedStateHandle: SavedStateHandle) :
  ViewModel() {
  val reLogin: Boolean = checkNotNull(savedStateHandle.get<Boolean>("reLogin"))

  private val _uiState = MutableStateFlow(initUiState())

  val uiState: StateFlow<LoginUiState> = _uiState

  fun onEvent(event: LoginEvent) {
    when (event) {
      is LoginEvent.LoginButtonPressed ->
        viewModelScope.launch { _uiState.update { loginRepository.login(_uiState.value) } }
      is LoginEvent.ServerChanged -> _uiState.update { it.copy(server = event.server) }
      is LoginEvent.UsernameChanged -> _uiState.update { it.copy(username = event.username) }
      is LoginEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
      LoginEvent.ErrorShown -> {
        _uiState.update { it.copy(loginState = GenericState.Idle) }
      }
    }
  }

  private fun initUiState(): LoginUiState {
    return if (reLogin) {
      runBlocking {
        val username = loginRepository.userPrefs.firstOrNull()?.username ?: ""
        val server = if (username.isNotBlank()) loginRepository.baseUrl.firstOrNull() ?: "" else ""
        LoginUiState(username = username, server = server, reLogin = true)
      }
    } else LoginUiState()
  }
}
