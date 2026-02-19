package dev.halim.shelfdroid.core.data.screen.usersettings

import android.text.format.DateUtils
import dev.halim.core.network.response.Session
import dev.halim.core.network.response.User as NetworkUser
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class UserSettingsRepository
@Inject
constructor(private val userRepo: UserRepo, private val helper: Helper) {

  suspend fun uiState(): UserSettingsUiState {
    val response =
      userRepo.remote("latestSession").getOrElse {
        return UserSettingsUiState(state = GenericState.Failure(it.message))
      }
    val users = response.users.map { user(it) }
    return UserSettingsUiState(users = users, state = GenericState.Success)
  }

  private fun user(user: NetworkUser): UserSettingsUiState.User {
    return UserSettingsUiState.User(
      id = user.id,
      username = user.username,
      type = UserType.toUserType(user.type.name),
      lastSeen = getRelativeTimeAndroid(user.lastSeen),
      isActive = user.isActive,
      lastSession = lastSession(user.latestSession),
      navPayload = navPayload(user),
    )
  }

  private fun navPayload(user: NetworkUser): NavUsersSettingsEditUser {
    return NavUsersSettingsEditUser(
      id = user.id,
      username = user.username,
      email = user.email ?: "",
      type = UserType.toUserType(user.type.name),
      isActive = user.isActive,
      download = user.permissions.download,
      update = user.permissions.update,
      delete = user.permissions.delete,
      upload = user.permissions.upload,
      createEReader = user.permissions.createEreader,
      accessExplicit = user.permissions.accessExplicitContent,
      accessAllLibraries = user.permissions.accessAllLibraries,
      accessAllTags = user.permissions.accessAllTags,
    )
  }

  private fun getRelativeTimeAndroid(timestampMs: Long?): String {
    if (timestampMs == null) return ""
    return DateUtils.getRelativeTimeSpanString(
        timestampMs,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
      )
      .toString()
  }

  private fun lastSession(session: Session?): UserSettingsUiState.LastSession {
    if (session == null) return UserSettingsUiState.LastSession()
    return UserSettingsUiState.LastSession(
      title = session.mediaMetadata.title ?: "",
      helper.formatSessionTimeRange(session.startedAt, session.updatedAt),
    )
  }
}
