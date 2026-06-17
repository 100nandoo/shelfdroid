package dev.halim.shelfdroid.core.ui.screen.usersettings.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserRepository
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserState
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserUiState
import dev.halim.shelfdroid.core.navigation.NavEditUser
import dev.halim.shelfdroid.core.ui.navigation.EditUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditUserViewModel.Factory::class)
class EditUserViewModel
@AssistedInject
constructor(@Assisted navKey: EditUser, private val repository: EditUserRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(repository.item(navKey.payload))
  private val isCreateMode = _uiState.value.editUser.isCreateMode()

  val uiState: StateFlow<EditUserUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EditUserUiState())

  private val _events = MutableSharedFlow<GenericUiEvent>()
  val events = _events.asSharedFlow()

  fun onEvent(event: EditUserEvent) {
    when (event) {
      is EditUserEvent.Update -> {
        _uiState.update { it.copy(editUser = event.transform(it.editUser)) }
      }
      is EditUserEvent.UpdateUiState -> {
        _uiState.update { event.transform(it) }
      }

      EditUserEvent.Submit -> submit()
    }
  }

  private fun submit() = viewModelScope.launch {
    _uiState.update { it.copy(state = EditUserState.Loading) }

    val validation = validatePermissions()
    if (validation != EditUserState.Success) {
      handleValidationError(validation)
      return@launch
    }

    if (isCreateMode) {
      val infoValidation = validateUserInfo()
      if (infoValidation != EditUserState.Success) {
        handleValidationError(infoValidation)
        return@launch
      }

      _uiState.update { repository.createUser(_uiState.value, _events) }
    } else {
      _uiState.update { repository.updateUser(_uiState.value, _events) }
    }
  }

  private suspend fun handleValidationError(state: EditUserState) {
    _uiState.update { it.copy(state = state) }
    _events.emit(GenericUiEvent.ShowErrorSnackbar())
  }

  private fun validatePermissions(): EditUserState {
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

  private fun validateUserInfo(): EditUserState {
    return createUserInfoValidation(_uiState.value)
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: EditUser): EditUserViewModel
  }
}

sealed interface EditUserEvent {

  data class Update(val transform: (NavEditUser) -> NavEditUser) : EditUserEvent

  data class UpdateUiState(val transform: (EditUserUiState) -> EditUserUiState) : EditUserEvent

  data object Submit : EditUserEvent
}

internal fun createUserInfoValidation(ui: EditUserUiState): EditUserState {
  val usernameValid = ui.editUser.username.isNotEmpty()
  val passwordValid = ui.editUser.password.isNotEmpty()

  return when {
    usernameValid && passwordValid -> EditUserState.Success
    !usernameValid && !passwordValid -> EditUserState.UsernameAndPasswordFieldError
    !passwordValid -> EditUserState.PasswordFieldError
    else -> EditUserState.UsernameFieldError
  }
}
