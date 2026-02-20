package dev.halim.shelfdroid.core.ui.screen.usersettings.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.UserSettingsEditUserRepository
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.UserSettingsEditUserUiState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UserSettingsEditUserViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: UserSettingsEditUserRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(repository.item(savedStateHandle.toRoute()))
  val uiState: StateFlow<UserSettingsEditUserUiState> =
    _uiState
      .asStateFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserSettingsEditUserUiState())

  fun onEvent(event: UserSettingsEditUserEvent) {
    when (event) {
      is UserSettingsEditUserEvent.Update -> {
        _uiState.update { it.copy(editUser = event.transform(it.editUser)) }
      }
      is UserSettingsEditUserEvent.UpdateUiState -> {
        _uiState.update { event.transform(it) }
      }

      UserSettingsEditUserEvent.Submit -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = GenericState.Loading) }
          _uiState.update { repository.updateUser(_uiState.value) }
        }
      }
    }
  }
}

sealed interface UserSettingsEditUserEvent {

  data class Update(val transform: (NavUsersSettingsEditUser) -> NavUsersSettingsEditUser) :
    UserSettingsEditUserEvent

  data class UpdateUiState(
    val transform: (UserSettingsEditUserUiState) -> UserSettingsEditUserUiState
  ) : UserSettingsEditUserEvent

  data object Submit : UserSettingsEditUserEvent
}
