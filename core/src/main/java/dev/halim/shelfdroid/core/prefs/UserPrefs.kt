package dev.halim.shelfdroid.core.prefs

import dev.halim.shelfdroid.core.UserType
import kotlinx.serialization.Serializable

@Serializable
data class UserPrefs(
  val id: String = "",
  val username: String = "",
  val type: UserType = UserType.Unknown,
  val isAdmin: Boolean = false,
  val download: Boolean = false,
  val update: Boolean = false,
  val delete: Boolean = false,
  val upload: Boolean = false,
  val accessToken: String = "",
  val refreshToken: String = "",
)
