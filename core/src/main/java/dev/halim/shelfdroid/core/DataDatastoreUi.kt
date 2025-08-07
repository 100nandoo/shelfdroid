package dev.halim.shelfdroid.core

import kotlinx.serialization.Serializable

@Serializable
data class UserPrefs(
  val id: String = "",
  val username: String = "",
  val isAdmin: Boolean = false,
  val download: Boolean = false,
  val update: Boolean = false,
  val delete: Boolean = false,
  val upload: Boolean = false,
  val accessToken: String = "",
  val refreshToken: String = "",
)

@Serializable data class ServerPrefs(val version: String = "")
