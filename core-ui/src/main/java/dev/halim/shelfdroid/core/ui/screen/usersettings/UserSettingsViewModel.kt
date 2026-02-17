package dev.halim.shelfdroid.core.ui.screen.usersettings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsRepository
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class UserSettingsViewModel @Inject constructor(private val repository: UserSettingsRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(UserSettingsUiState())
  val uiState: StateFlow<UserSettingsUiState> = _uiState.asStateFlow()

  fun onEvent(event: UserSettingsEvent) {
    when (event) {
      UserSettingsEvent.OnInit -> {}
    }
  }
}

sealed interface UserSettingsEvent {

  data object OnInit : UserSettingsEvent
}
