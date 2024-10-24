package dev.halim.shelfdroid.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreKeys
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.login.LoginRequest
import dev.halim.shelfdroid.network.login.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(
    private val api: Api,
    private val dataStore: DataStore<Preferences>,
    private val dataStoreKeys: DataStoreKeys
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        viewModelScope.launch {
            dataStore.data.map { pref ->
                pref[dataStoreKeys.TOKEN] ?: ""
            }.collect { token ->
                _uiState.value = _uiState.value.copy(token = token)
            }
        }
    }

    fun onEvent(loginEvent: LoginEvent) {
        when (loginEvent) {
            is LoginEvent.LoginButtonPressed -> viewModelScope.launch {
                val response = login()
                val token = response.user.token
                if (token.isNotBlank()) {
                    dataStore.edit { it[dataStoreKeys.TOKEN] = token }
                }
                _uiState.value = _uiState.value.copy(token = token)
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
    val isLoading: Boolean = false,
    val token: String = ""
)

sealed class LoginEvent {
    data object LoginButtonPressed : LoginEvent()
}
