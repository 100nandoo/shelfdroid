package dev.halim.shelfdroid.core.data.screen.usersettings

import android.text.format.DateUtils
import dev.halim.core.network.response.Permissions as NetworkPermissions
import dev.halim.core.network.response.Session
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.database.UserEntity
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.core.navigation.NavEditUser
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.serialization.json.Json

class UserSettingsMapper @Inject constructor(private val helper: Helper) {
  fun user(entity: UserEntity): UserSettingsUiState.User {
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

  private fun navPayload(entity: UserEntity): NavEditUser {
    val permissions = Json.decodeFromString(NetworkPermissions.serializer(), entity.permissions)
    return NavEditUser(
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
