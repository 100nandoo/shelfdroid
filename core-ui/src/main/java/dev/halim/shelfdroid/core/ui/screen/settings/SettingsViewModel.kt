package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.auth.AuthStateRepository
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.data.screen.settings.SettingsState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import dev.halim.shelfdroid.core.data.sessionreset.SessionResetRepository
import dev.halim.shelfdroid.core.prefs.Prefs
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val sessionResetRepository: SessionResetRepository,
  private val authStateRepository: AuthStateRepository,
  @Named("version") val version: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  private val _events = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
  val uiState: StateFlow<SettingsUiState> =
    combine(
        _uiState,
        settingsRepository.darkMode,
        settingsRepository.dynamicTheme,
        settingsRepository.prefs,
      ) { uiState: SettingsUiState, isDarkMode: Boolean, isDynamicTheme: Boolean, prefs: Prefs ->
        uiState.copy(
          isDarkMode = isDarkMode,
          isDynamicTheme = isDynamicTheme,
          isAdmin = prefs.userPrefs.isAdmin,
          username = prefs.userPrefs.username,
          canDelete = prefs.userPrefs.isAdmin || prefs.userPrefs.delete,
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())
  val events = _events.asSharedFlow()

  fun onEvent(event: SettingsEvent) {
    when (event) {
      is SettingsEvent.LogoutButtonPressed -> viewModelScope.launch { logout() }
      SettingsEvent.ReLoginButtonPressed -> {
        viewModelScope.launch { authStateRepository.startManualReLogin() }
      }
      is SettingsEvent.SwitchDarkTheme -> {
        viewModelScope.launch { settingsRepository.updateDarkMode(event.isDarkMode) }
      }
      is SettingsEvent.SwitchDynamicTheme -> {
        viewModelScope.launch { settingsRepository.updateDynamicTheme(event.isDynamic) }
      }
    }
  }

  private suspend fun logout() {
    _uiState.update { it.copy(settingsState = SettingsState.Loading) }
    sessionResetRepository.fullLogout().apply {
      onSuccess {
        _uiState.update { it.copy(settingsState = SettingsState.Success) }
        _events.tryEmit(SettingsUiEvent.LoggedOut)
      }
      onFailure { error ->
        _uiState.update { it.copy(settingsState = SettingsState.Failure(error.message)) }
      }
    }
  }
}

sealed interface SettingsEvent {
  data object LogoutButtonPressed : SettingsEvent

  data object ReLoginButtonPressed : SettingsEvent

  data class SwitchDarkTheme(val isDarkMode: Boolean) : SettingsEvent

  data class SwitchDynamicTheme(val isDynamic: Boolean) : SettingsEvent
}

sealed interface SettingsUiEvent {
  data object LoggedOut : SettingsUiEvent
}
