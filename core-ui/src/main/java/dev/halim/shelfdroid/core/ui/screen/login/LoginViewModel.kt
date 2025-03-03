package dev.halim.shelfdroid.core.ui.screen.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.core.datastore.DataStoreManager
//import dev.halim.shelfdroid.network.Api
//import dev.halim.shelfdroid.network.LoginRequest
//import dev.halim.shelfdroid.network.LoginResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
//    private val api: Api,
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
//        val request = LoginRequest(_uiState.value.username, _uiState.value.password)
//        val result = api.login(request)
//        result.onSuccess { response ->
//            viewModelScope.launch {
//                dataStoreManager.updateBaseUrl(_uiState.value.server)
//                dataStoreManager.updateToken(response.user.token)
//                dataStoreManager.updateUserId(response.user.id)
//                dataStoreManager.generateDeviceId()
//            }
//
//            _uiState.update { it.copy(loginState = LoginState.Success) }
//        }
//        result.onFailure { error -> _uiState.update { it.copy(loginState = LoginState.Failure(error.message)) } }
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
