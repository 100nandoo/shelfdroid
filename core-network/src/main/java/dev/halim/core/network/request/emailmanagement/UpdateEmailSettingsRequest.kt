package dev.halim.core.network.request.emailmanagement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateEmailSettingsRequest(
  @SerialName("host") val host: String? = null,
  @SerialName("port") val port: Int? = null,
  @SerialName("secure") val secure: Boolean? = null,
  @SerialName("rejectUnauthorized") val rejectUnauthorized: Boolean? = null,
  @SerialName("user") val user: String? = null,
  @SerialName("pass") val pass: String? = null,
  @SerialName("testAddress") val testAddress: String? = null,
  @SerialName("fromAddress") val fromAddress: String? = null,
)
