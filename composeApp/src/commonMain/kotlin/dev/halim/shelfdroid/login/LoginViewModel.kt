package dev.halim.shelfdroid.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.login.LoginRequest
import dev.halim.shelfdroid.network.login.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LoginViewModel(private val api: Api) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    private val jsonFormatter = Json { prettyPrint = true }

    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEvent(loginEvent: LoginEvent) {
        when (loginEvent) {
            is LoginEvent.LoginButtonPressed -> viewModelScope.launch {
                val responseJson = jsonFormatter.encodeToString(login())
                _uiState.value = _uiState.value.copy(responseJson = responseJson)
            }
        }
    }

    fun updateUiState(newState: LoginUiState) {
        _uiState.value = newState
    }

    suspend fun login(): LoginResponse {
        Api.baseUrl = _uiState.value.server
        return withContext(Dispatchers.IO) {
            runCatching {
                api.login(
                    LoginRequest(
                        _uiState.value.username,
                        _uiState.value.password
                    )
                )
            }
                .getOrNull() ?: LoginResponse()
        }
    }
}

data class LoginUiState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val responseJson: String = ""
)

sealed class LoginEvent {
    data object LoginButtonPressed : LoginEvent()
}
