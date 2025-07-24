package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.response.User
import dev.halim.core.network.response.UserType
import dev.halim.shelfdroid.core.UserPrefs
import javax.inject.Inject

class LoginMapper @Inject constructor() {
  fun toUserPrefs(user: User): UserPrefs {
    return UserPrefs(
      id = user.id,
      username = user.username,
      isAdmin = user.type == UserType.ADMIN || user.type == UserType.ROOT,
      download = user.permissions.download,
      upload = user.permissions.upload,
      delete = user.permissions.delete,
      update = user.permissions.update,
      accessToken = user.accessToken,
      refreshToken = user.refreshToken,
    )
  }
}
