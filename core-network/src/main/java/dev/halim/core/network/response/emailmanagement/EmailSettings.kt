package dev.halim.core.network.response.emailmanagement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailSettingsResponse(
  @SerialName("settings") val settings: EmailSettings = EmailSettings()
)

@Serializable
data class EreaderDevicesResponse(
  @SerialName("ereaderDevices") val ereaderDevices: List<EreaderDevice> = emptyList()
)

@Serializable
data class EmailSettings(
  @SerialName("id") val id: String = "",
  @SerialName("host") val host: String? = null,
  @SerialName("port") val port: Int = 465,
  @SerialName("secure") val secure: Boolean = true,
  @SerialName("rejectUnauthorized") val rejectUnauthorized: Boolean = true,
  @SerialName("user") val user: String? = null,
  @SerialName("pass") val pass: String? = null,
  @SerialName("testAddress") val testAddress: String? = null,
  @SerialName("fromAddress") val fromAddress: String? = null,
  @SerialName("ereaderDevices") val ereaderDevices: List<EreaderDevice> = emptyList(),
)

@Serializable
data class EreaderDevice(
  @SerialName("name") val name: String = "",
  @SerialName("email") val email: String = "",
  @SerialName("availabilityOption") val availabilityOption: String = "adminOrUp",
  @SerialName("users") val users: List<String> = emptyList(),
)
