package dev.halim.shelfdroid.core.data.screen.usersettings.edit

import dev.halim.shelfdroid.core.Permissions
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser

data class UserSettingsEditUserUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: GenericState = GenericState.Idle,
  val editUser: NavUsersSettingsEditUser = NavUsersSettingsEditUser(),
  val permissions: Permissions = Permissions(),
  val tags: List<String> = emptyList(),
)
