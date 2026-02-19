package dev.halim.shelfdroid.core.data.screen.usersettings.edit

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser

data class UserSettingsEditUserUiState(
  val state: GenericState = GenericState.Loading,
  val editUser: NavUsersSettingsEditUser = NavUsersSettingsEditUser(),
)
