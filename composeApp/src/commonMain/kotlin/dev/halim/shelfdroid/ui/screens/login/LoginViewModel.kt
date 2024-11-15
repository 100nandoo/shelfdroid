package dev.halim.shelfdroid.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val api: Api,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEvent(loginEvent: LoginEvent) {
        when (loginEvent) {
            is LoginEvent.LoginButtonPressed -> viewModelScope.launch {
                login()
            }
        }
    }

    fun updateUiState(newState: LoginUiState) {
        _uiState.value = newState
    }

    private suspend fun login() {
        Api.baseUrl = _uiState.value.server
        val request = LoginRequest(_uiState.value.username, _uiState.value.password)
        api.login(request).collect { result ->
            result.onSuccess { response ->
                Api.baseUrl = _uiState.value.server
                dataStoreManager.setBaseUrl(_uiState.value.server)
                dataStoreManager.setToken(response.user.token)
                dataStoreManager.generateDeviceId()
                _uiState.value = _uiState.value.copy(loginState = LoginState.Success)
            }
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(loginState = LoginState.Failure(error.message))
            }
        }
    }
}

data class LoginUiState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val loginState: LoginState = LoginState.NotLoggedIn
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
