package dev.halim.shelfdroid.core.ui.screen.usersettings.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserRepository
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserState
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserUiState
import dev.halim.shelfdroid.core.navigation.NavEditUser
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditUserViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: EditUserRepository) :
  ViewModel() {
  private val _uiState = MutableStateFlow(repository.item(savedStateHandle.toRoute()))
  val uiState: StateFlow<EditUserUiState> =
    _uiState
      .asStateFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EditUserUiState())

  private val _events = MutableSharedFlow<GenericUiEvent>()
  val events = _events.asSharedFlow()

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
          _uiState.update { it.copy(state = EditUserState.Loading) }

          val state = validateForm()
          if (state is EditUserState.Success) {
            _uiState.update { repository.updateUser(_uiState.value, _events) }
          } else {
            _uiState.update { it.copy(state = state) }
            viewModelScope.launch { _events.emit(GenericUiEvent.ShowErrorSnackbar()) }
          }
        }
      }
    }
  }

  private fun validateForm(): EditUserState {
    val ui = _uiState.value

    val tagsValid = ui.permissions.accessAllTags || ui.editUser.itemTagsAccessible.isNotEmpty()

    val librariesValid =
      ui.permissions.accessAllLibraries || ui.editUser.librariesAccessible.isNotEmpty()

    return when {
      tagsValid && librariesValid -> EditUserState.Success
      !tagsValid && !librariesValid -> EditUserState.LibrariesAndItemTagsFieldError
      !tagsValid -> EditUserState.ItemTagsFieldError
      else -> EditUserState.LibrariesFieldError
    }
  }
}

sealed interface UserSettingsEditUserEvent {

  data class Update(val transform: (NavEditUser) -> NavEditUser) : UserSettingsEditUserEvent

  data class UpdateUiState(val transform: (EditUserUiState) -> EditUserUiState) :
    UserSettingsEditUserEvent

  data object Submit : UserSettingsEditUserEvent
}
