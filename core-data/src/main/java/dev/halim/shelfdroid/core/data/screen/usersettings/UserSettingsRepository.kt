package dev.halim.shelfdroid.core.data.screen.usersettings

import android.text.format.DateUtils
import dev.halim.core.network.response.Permissions as NetworkPermissions
import dev.halim.core.network.response.Session
import dev.halim.core.network.response.User as NetworkUser
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.database.UserEntity
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.serialization.json.Json

class UserSettingsRepository
@Inject
constructor(private val userRepo: UserRepo, private val helper: Helper, private val json: Json) {

  suspend fun uiState(): UserSettingsUiState {
    val response =
      userRepo.remote("latestSession").getOrElse {
        return UserSettingsUiState(state = GenericState.Failure(it.message))
      }
    val users = response.users.map { user(it) }
    return UserSettingsUiState(users = users, state = GenericState.Success)
  }

  fun updateUser(userId: String, uiState: UserSettingsUiState): UserSettingsUiState {
    val entity = userRepo.byId(userId) ?: return uiState
    val updatedUser = user(entity)
    val users = uiState.users.map { if (it.id == userId) updatedUser else it }

    return uiState.copy(users = users)
  }

  suspend fun deleteUser(userId: String, uiState: UserSettingsUiState): UserSettingsUiState {
    userRepo.delete(userId).getOrElse {
      return uiState.copy(apiState = UserSettingsApiState.DeleteFailure(it.message))
    }
    val users = uiState.users.filter { it.id != userId }
    return uiState.copy(users = users, apiState = UserSettingsApiState.DeleteSuccess)
  }

  private fun user(entity: UserEntity): UserSettingsUiState.User {
    val lastSession = entity.latestSession?.let { Json.decodeFromString(Session.serializer(), it) }
    return UserSettingsUiState.User(
      id = entity.id,
      username = entity.username,
      type = UserType.toUserType(entity.type.name),
      lastSeen = getRelativeTimeAndroid(entity.lastSeen),
      isActive = entity.isActive.toBoolean(),
      lastSession = lastSession(lastSession),
      navPayload = navPayload(entity),
    )
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
    val permissions = json.encodeToString(NetworkPermissions.serializer(), user.permissions)
    return NavUsersSettingsEditUser(
      id = user.id,
      username = user.username,
      email = user.email ?: "",
      type = UserType.toUserType(user.type.name),
      isActive = user.isActive,
      librariesAccessible = user.librariesAccessible,
      itemTagsAccessible = user.itemTagsSelected,
      permissions = permissions,
      invert = user.permissions.selectedTagsNotAccessible,
    )
  }

  private fun navPayload(entity: UserEntity): NavUsersSettingsEditUser {
    val permissions = Json.decodeFromString(NetworkPermissions.serializer(), entity.permissions)
    return NavUsersSettingsEditUser(
      id = entity.id,
      username = entity.username,
      email = entity.email,
      type = UserType.toUserType(entity.type.name),
      isActive = entity.isActive.toBoolean(),
      librariesAccessible = entity.librariesAccessible,
      itemTagsAccessible = entity.itemTagsAccessible,
      permissions = entity.permissions,
      invert = permissions.selectedTagsNotAccessible,
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
