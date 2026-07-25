package dev.halim.core.network.request.apprisenotificationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppriseNotificationRuleRequest(
  @SerialName("libraryId") val libraryId: String? = null,
  @SerialName("eventName") val eventName: String,
  @SerialName("urls") val urls: List<String>,
  @SerialName("titleTemplate") val titleTemplate: String,
  @SerialName("bodyTemplate") val bodyTemplate: String,
  @SerialName("enabled") val enabled: Boolean,
  @SerialName("type") val type: String? = null,
)
