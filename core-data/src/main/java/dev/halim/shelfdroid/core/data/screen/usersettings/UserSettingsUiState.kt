package dev.halim.shelfdroid.core.data.screen.usersettings

import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser

data class UserSettingsUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: UserSettingsApiState = UserSettingsApiState.Idle,
  val isLoginUserRoot: Boolean = false,
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

sealed interface UserSettingsApiState {
  data object Idle : UserSettingsApiState

  data object Loading : UserSettingsApiState

  data object AddSuccess : UserSettingsApiState

  data class AddFailure(val message: String?) : UserSettingsApiState

  data object DeleteSuccess : UserSettingsApiState

  data class DeleteFailure(val message: String?) : UserSettingsApiState
}
