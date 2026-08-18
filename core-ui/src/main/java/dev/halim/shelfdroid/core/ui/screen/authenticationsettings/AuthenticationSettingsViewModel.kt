package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsRepository
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthenticationSettingsViewModel
@Inject
constructor(private val repository: AuthenticationSettingsRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(AuthenticationSettingsUiState())
  val uiState: StateFlow<AuthenticationSettingsUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        AuthenticationSettingsUiState(),
      )

  fun onEvent(event: AuthenticationSettingsEvent) {
    when (event) {
      AuthenticationSettingsEvent.Retry -> load()
    }
  }

  private fun load() {
    viewModelScope.launch {
      _uiState.update { AuthenticationSettingsUiState() }
      _uiState.update { repository.load() }
    }
  }
}

sealed interface AuthenticationSettingsEvent {
  data object Retry : AuthenticationSettingsEvent
}
