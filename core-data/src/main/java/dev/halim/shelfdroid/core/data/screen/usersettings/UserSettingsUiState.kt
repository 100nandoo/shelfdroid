package dev.halim.shelfdroid.core.data.screen.usersettings

import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser

data class UserSettingsUiState(
  val state: GenericState = GenericState.Loading,
  val users: List<User> = emptyList(),
) {

  data class User(
    val id: String = "",
    val username: String = "",
    val type: UserType = UserType.Unknown,
    val lastSeen: String = "",
    val isActive: Boolean = false,
    val lastSession: LastSession = LastSession(),
    val navPayload: NavUsersSettingsEditUser = NavUsersSettingsEditUser(),
  )

  data class LastSession(val title: String = "", val timeRange: String = "")
}
