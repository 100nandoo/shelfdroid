package dev.halim.shelfdroid.ui.screens.login

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.BASE_URL
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.TOKEN
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.UnauthorizedException
import dev.halim.shelfdroid.network.login.LoginRequest
import dev.halim.shelfdroid.network.login.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    suspend fun login() {
        Api.baseUrl = _uiState.value.server
        _uiState.value = _uiState.value.copy(loginState = LoginState.Loading)
        return withContext(Dispatchers.IO) {
            runCatching {
                api.login(LoginRequest(_uiState.value.username, _uiState.value.password))
            }
                .onSuccess { loginResponse: LoginResponse ->
                    dataStoreManager.dataStore.edit {
                        it[BASE_URL] = _uiState.value.server
                        it[TOKEN] = loginResponse.user.token
                    }
                    _uiState.value =
                        _uiState.value.copy(loginState = LoginState.Success(loginResponse.user.token))
                }
                .onFailure { throwable ->
                    val errorMessage = when (throwable) {
                        is UnauthorizedException -> "Invalid username or password."
                        else -> "Unknown error occurred"
                    }
                    _uiState.value =
                        _uiState.value.copy(loginState = LoginState.Failure(errorMessage))
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
    data class Success(val token: String) : LoginState()
    data class Failure(val errorMessage: String) : LoginState()
}

sealed class LoginEvent {
    data object LoginButtonPressed : LoginEvent()
}
