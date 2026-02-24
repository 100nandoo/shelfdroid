package dev.halim.shelfdroid.core.data.screen.usersettings.edit

import dev.halim.shelfdroid.core.Permissions
import dev.halim.shelfdroid.core.navigation.NavEditUser

data class EditUserUiState(
  val state: EditUserState = EditUserState.Loading,
  val editUser: NavEditUser = NavEditUser(),
  val permissions: Permissions = Permissions(),
  val tags: List<String> = emptyList(),
  val libraries: List<Library> = emptyList(),
) {
  data class Library(val id: String, val name: String)
}

sealed interface EditUserState {
  data object Loading : EditUserState

  data object Success : EditUserState

  data object ApiUpdateSuccess : EditUserState

  data object ApiUpdateError : EditUserState

  data object ApiCreateSuccess : EditUserState

  data object ApiCreateError : EditUserState

  data object Idle : EditUserState

  data object LibrariesFieldError : EditUserState

  data object ItemTagsFieldError : EditUserState

  data object LibrariesAndItemTagsFieldError : EditUserState

  data object UsernameFieldError : EditUserState

  data object PasswordFieldError : EditUserState

  data object UsernameAndPasswordFieldError : EditUserState
}
