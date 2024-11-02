package dev.halim.shelfdroid.ui.screens.settings

import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val api: Api,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            dataStoreManager.dataStore.data.collectLatest { preferences ->
                val isDarkMode = preferences[DataStoreManager.DataStoreKeys.IS_DARK_MODE] ?: false
                _uiState.value = _uiState.value.copy(isDarkMode = isDarkMode)
            }
        }
    }

    fun onEvent(settingsEvent: SettingsEvent) {
        when (settingsEvent) {
            is SettingsEvent.LogoutButtonPressed -> viewModelScope.launch {
                logout()
            }

            is SettingsEvent.SwitchToggle -> {
                viewModelScope.launch {
                    try {
                        dataStoreManager.dataStore.edit { preferences ->
                            preferences[DataStoreManager.DataStoreKeys.IS_DARK_MODE] = settingsEvent.isDarkMode
                        }
                        _uiState.value = _uiState.value.copy(isDarkMode = settingsEvent.isDarkMode)
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            settingsState = SettingsState.Failure("Failed to update dark mode setting")
                        )
                    }
                }
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
    val settingsState: SettingsState = SettingsState.NotLoggedOut,
    val isDarkMode: Boolean = false
)

sealed class SettingsState {
    data object NotLoggedOut : SettingsState()
    data object Loading : SettingsState()
    data object Success : SettingsState()
    data class Failure(val errorMessage: String) : SettingsState()
}

sealed class SettingsEvent {
    data object LogoutButtonPressed : SettingsEvent()
    data class SwitchToggle(val isDarkMode: Boolean) : SettingsEvent()
}
