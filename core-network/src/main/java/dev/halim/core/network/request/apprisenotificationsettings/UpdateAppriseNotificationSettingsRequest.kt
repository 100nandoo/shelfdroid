package dev.halim.core.network.request.apprisenotificationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateAppriseNotificationSettingsRequest(
  @SerialName("appriseApiUrl") val appriseApiUrl: String,
  @SerialName("maxNotificationQueue") val maxNotificationQueue: Int,
  @SerialName("maxFailedAttempts") val maxFailedAttempts: Int,
)
