package dev.halim.core.network.response

import dev.halim.core.network.response.login.ServerSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerSettingsResponse(
  @SerialName("serverSettings") val serverSettings: ServerSettings = ServerSettings()
)
