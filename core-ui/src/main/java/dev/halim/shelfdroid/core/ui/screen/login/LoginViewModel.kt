package dev.halim.shelfdroid.core.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.login.LoginRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginRepository: LoginRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(LoginUiState())

  val uiState: StateFlow<LoginUiState> = _uiState

  fun onEvent(loginEvent: LoginEvent) {
    when (loginEvent) {
      is LoginEvent.LoginButtonPressed -> viewModelScope.launch { login() }
    }
  }

  fun updateUiState(newState: LoginUiState) {
    _uiState.value = newState
  }

  private suspend fun login() {
    loginRepository
      .login(_uiState.value.server, _uiState.value.username, _uiState.value.password)
      .apply {
        onSuccess { _uiState.update { it.copy(loginState = LoginState.Success) } }
        onFailure { error ->
          _uiState.update { it.copy(loginState = LoginState.Failure(error.message)) }
        }
      }
  }
}

data class LoginUiState(
  val server: String = "",
  val username: String = "",
  val password: String = "",
  val loginState: LoginState = LoginState.NotLoggedIn,
)

sealed class LoginState {
  data object NotLoggedIn : LoginState()

  data object Loading : LoginState()

  data object Success : LoginState()

  data class Failure(val errorMessage: String?) : LoginState()
}

sealed class LoginEvent {
  data object LoginButtonPressed : LoginEvent()
}
