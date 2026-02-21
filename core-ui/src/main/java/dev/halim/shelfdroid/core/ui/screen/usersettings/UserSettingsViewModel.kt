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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UserSettingsViewModel @Inject constructor(private val repository: UserSettingsRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(UserSettingsUiState())
  val uiState: StateFlow<UserSettingsUiState> =
    _uiState
      .asStateFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserSettingsUiState())

  init {
    viewModelScope.launch { _uiState.value = repository.uiState() }
  }

  fun onEvent(event: UserSettingsEvent) {
    when (event) {
      UserSettingsEvent.AddUser -> TODO()
      is UserSettingsEvent.DeleteUser -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = UserSettingsApiState.Loading) }
          _uiState.update { repository.deleteUser(event.user.id, it) }
        }
      }
      is UserSettingsEvent.UserInfo -> {}
      is UserSettingsEvent.UpdateUser -> {
        viewModelScope.launch { _uiState.update { repository.updateUser(event.userId, it) } }
      }
    }
  }
}

sealed interface UserSettingsEvent {

  data object AddUser : UserSettingsEvent

  data class UserInfo(val user: UserSettingsUiState.User) : UserSettingsEvent

  data class DeleteUser(val user: UserSettingsUiState.User) : UserSettingsEvent

  data class UpdateUser(val userId: String) : UserSettingsEvent
}
