package dev.halim.shelfdroid.screen.settings

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val api: Api,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = _uiState

    fun onEvent(settingsEvent: SettingsEvent) {
        when (settingsEvent) {
            is SettingsEvent.LogoutButtonPressed -> viewModelScope.launch {
                logout()
            }
        }
    }

    fun updateUiState(newState: SettingsUiState) {
        _uiState.value = newState
    }

    private fun logout() {
        _uiState.value = _uiState.value.copy(settingsState = SettingsState.Loading)
        viewModelScope.launch {
            api.handleApiCall(
                successStateUpdater = {
                    dataStoreManager.dataStore.edit { it.clear() }
                    _uiState.value = _uiState.value.copy(settingsState = SettingsState.Success)
                },
                errorStateUpdater = { errorMessage ->
                    _uiState.value =
                        _uiState.value.copy(settingsState = SettingsState.Failure(errorMessage))
                },
                apiCall = { api.logout() }
            )
        }
    }
}

data class SettingsUiState(
    val settingsState: SettingsState = SettingsState.NotLoggedOut
)

sealed class SettingsState {
    data object NotLoggedOut : SettingsState()
    data object Loading : SettingsState()
    data object Success : SettingsState()
    data class Failure(val errorMessage: String) : SettingsState()
}

sealed class SettingsEvent {
    data object LogoutButtonPressed : SettingsEvent()
}
