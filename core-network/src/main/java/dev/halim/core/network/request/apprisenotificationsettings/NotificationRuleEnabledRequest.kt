package dev.halim.core.network.request.apprisenotificationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationRuleEnabledRequest(
  @SerialName("id") val id: String,
  @SerialName("enabled") val enabled: Boolean,
)
