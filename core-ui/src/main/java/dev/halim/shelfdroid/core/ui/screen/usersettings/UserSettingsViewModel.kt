package dev.halim.shelfdroid.core.ui.screen.usersettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsApiState
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsRepository
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UserSettingsViewModel @Inject constructor(private val repository: UserSettingsRepository) :
  ViewModel() {

  private val _uiState: MutableStateFlow<UserSettingsApiState> =
    MutableStateFlow(UserSettingsApiState.Idle)
  val uiState: StateFlow<UserSettingsUiState> =
    combine(_uiState, repository.item()) { apiState, userSettingsUiState ->
        userSettingsUiState.copy(apiState = apiState)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserSettingsUiState())

  init {
    viewModelScope.launch { repository.remote() }
  }

  fun onEvent(event: UserSettingsEvent) {
    when (event) {
      UserSettingsEvent.AddUser -> TODO()
      is UserSettingsEvent.DeleteUser -> {
        viewModelScope.launch {
          _uiState.update { UserSettingsApiState.Loading }
          _uiState.update { repository.deleteUser(event.user.id) }
        }
      }
      is UserSettingsEvent.UserInfo -> {}
    }
  }
}

sealed interface UserSettingsEvent {

  data object AddUser : UserSettingsEvent

  data class UserInfo(val user: UserSettingsUiState.User) : UserSettingsEvent

  data class DeleteUser(val user: UserSettingsUiState.User) : UserSettingsEvent
}
