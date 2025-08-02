package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.data.screen.settings.SettingsState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(private val repository: SettingsRepository, @Named("version") val version: String) :
  ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> =
    combine(
        _uiState,
        repository.darkMode,
        repository.dynamicTheme,
        repository.listView,
        repository.userPrefs,
      ) {
        uiState: SettingsUiState,
        isDarkMode: Boolean,
        isDynamicTheme: Boolean,
        isListView: Boolean,
        userPrefs: UserPrefs ->
        uiState.copy(
          isDarkMode = isDarkMode,
          isDynamicTheme = isDynamicTheme,
          isListView = isListView,
          isAdmin = userPrefs.isAdmin,
          username = userPrefs.username,
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

  fun onEvent(event: SettingsEvent) {
    when (event) {
      is SettingsEvent.LogoutButtonPressed -> viewModelScope.launch { logout() }

      is SettingsEvent.SwitchDarkTheme -> {
        viewModelScope.launch { repository.updateDarkMode(event.isDarkMode) }
      }

      is SettingsEvent.SwitchDynamicTheme -> {
        viewModelScope.launch { repository.updateDynamicTheme(event.isDynamic) }
      }
      is SettingsEvent.SwitchListView -> {
        viewModelScope.launch { repository.updateListView(event.isListView) }
      }
    }
  }

  private suspend fun logout() {
    repository.logout().apply {
      onSuccess { _uiState.update { it.copy(settingsState = SettingsState.Success) } }
      onFailure { error ->
        _uiState.update { it.copy(settingsState = SettingsState.Failure(error.message)) }
      }
    }
  }
}

sealed class SettingsEvent {
  data object LogoutButtonPressed : SettingsEvent()

  data class SwitchDarkTheme(val isDarkMode: Boolean) : SettingsEvent()

  data class SwitchDynamicTheme(val isDynamic: Boolean) : SettingsEvent()

  data class SwitchListView(val isListView: Boolean) : SettingsEvent()
}
