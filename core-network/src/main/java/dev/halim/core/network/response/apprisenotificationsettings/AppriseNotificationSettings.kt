package dev.halim.core.network.response.apprisenotificationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppriseNotificationSettingsResponse(
  @SerialName("settings") val settings: AppriseNotificationSettings = AppriseNotificationSettings(),
  @SerialName("data") val data: AppriseNotificationData = AppriseNotificationData(),
)

@Serializable
data class AppriseNotificationData(
  @SerialName("events") val events: List<AppriseNotificationEvent> = emptyList()
)

@Serializable
data class AppriseNotificationEvent(
  @SerialName("name") val name: String = "",
  @SerialName("description") val description: String = "",
  @SerialName("variables") val variables: List<String> = emptyList(),
  @SerialName("defaults")
  val defaults: AppriseNotificationEventDefaults = AppriseNotificationEventDefaults(),
)

@Serializable
data class AppriseNotificationEventDefaults(
  @SerialName("title") val title: String = "",
  @SerialName("body") val body: String = "",
)

@Serializable
data class AppriseNotificationSettings(
  @SerialName("id") val id: String = "",
  @SerialName("appriseType") val appriseType: String? = null,
  @SerialName("appriseApiUrl") val appriseApiUrl: String? = null,
  @SerialName("notifications") val notifications: List<AppriseNotificationRule> = emptyList(),
  @SerialName("maxFailedAttempts") val maxFailedAttempts: Int = 5,
  @SerialName("maxNotificationQueue") val maxNotificationQueue: Int = 10,
  @SerialName("notificationDelay") val notificationDelay: Int = 0,
)

@Serializable
data class AppriseNotificationRule(
  @SerialName("id") val id: String = "",
  @SerialName("libraryId") val libraryId: String? = null,
  @SerialName("eventName") val eventName: String = "",
  @SerialName("urls") val urls: List<String> = emptyList(),
  @SerialName("titleTemplate") val titleTemplate: String = "",
  @SerialName("bodyTemplate") val bodyTemplate: String = "",
  @SerialName("enabled") val enabled: Boolean = true,
  @SerialName("type") val type: String? = null,
  @SerialName("lastFiredAt") val lastFiredAt: Long? = null,
  @SerialName("lastAttemptFailed") val lastAttemptFailed: Boolean = false,
  @SerialName("numConsecutiveFailedAttempts") val numConsecutiveFailedAttempts: Int = 0,
  @SerialName("numTimesFired") val numTimesFired: Int = 0,
  @SerialName("createdAt") val createdAt: Long? = null,
)
