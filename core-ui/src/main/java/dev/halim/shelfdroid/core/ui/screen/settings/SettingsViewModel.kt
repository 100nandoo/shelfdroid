package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.darkMode.collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LogoutButtonPressed -> viewModelScope.launch {
                logout()
            }

            is SettingsEvent.SwitchToggle -> {
                viewModelScope.launch {
                    repository.updateDarkMode(event.isDarkMode)
                }
            }
        }
    }

    private suspend fun logout() {
        repository.logout()
            .apply {
                onSuccess { _uiState.update { it.copy(settingsState = SettingsState.Success) } }
                onFailure { error -> _uiState.update { it.copy(settingsState = SettingsState.Failure(error.message)) } }
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
