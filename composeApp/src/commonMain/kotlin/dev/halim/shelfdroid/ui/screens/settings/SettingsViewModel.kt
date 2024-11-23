package dev.halim.shelfdroid.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.store.StoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val api: Api,
    private val dataStoreManager: DataStoreManager,
    private val storeManager: StoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            dataStoreManager.isDarkMode.collectLatest { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
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
                    dataStoreManager.setDarkMode(settingsEvent.isDarkMode)
                }
            }
        }
    }

    private suspend fun logout() {
        val result = api.logout()
        result.onSuccess { _ ->
            dataStoreManager.clear()
            storeManager.clear()
            _uiState.update { it.copy(settingsState = SettingsState.Success) }
        }
        result.onFailure { error ->
            _uiState.update { it.copy(settingsState = SettingsState.Failure(error.message)) }
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
    data class Failure(val errorMessage: String?) : SettingsState()
}

sealed class SettingsEvent {
    data object LogoutButtonPressed : SettingsEvent()
    data class SwitchToggle(val isDarkMode: Boolean) : SettingsEvent()
}
