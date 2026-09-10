package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

@Serializable
enum class ServerAccessMode {
  Internet,
  LocalNetwork,
}

@Serializable
data class ServerPrefs(
  val version: String = "",
  val logLevel: Int = 1,
  val accessMode: ServerAccessMode = ServerAccessMode.Internet,
)
