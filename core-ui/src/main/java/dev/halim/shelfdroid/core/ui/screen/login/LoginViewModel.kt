package dev.halim.shelfdroid.core.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.core.network.ApiService
import dev.halim.core.network.LoginRequest
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: ApiService,
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
        DataStoreManager.Companion.BASE_URL = _uiState.value.server
        val request = LoginRequest(_uiState.value.username, _uiState.value.password)
        val result = api.login(request)
        result.onSuccess { response ->
            viewModelScope.launch {
                DataStoreManager.Companion.BASE_URL = _uiState.value.server
                dataStoreManager.updateToken(response.user.token)
                dataStoreManager.updateUserId(response.user.id)
                dataStoreManager.generateDeviceId()
            }

            _uiState.update { it.copy(loginState = LoginState.Success) }
        }
        result.onFailure { error -> _uiState.update { it.copy(loginState = LoginState.Failure(error.message)) } }
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
