package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.shelfdroid.core.data.GenericState

data class AppriseNotificationSettingsUiState(
  val state: GenericState = GenericState.Loading,
  val settings: AppriseGlobalSettingsUi = AppriseGlobalSettingsUi(),
  val notificationRules: List<NotificationRuleUi> = emptyList(),
)

data class AppriseGlobalSettingsUi(
  val appriseApiUrl: String = "",
  val maxNotificationQueue: String = "",
  val maxFailedAttempts: String = "",
)

data class NotificationRuleUi(
  val id: String,
  val eventName: String,
  val enabled: Boolean,
  val destinationSummary: String,
  val status: NotificationRuleStatus,
  val statusValue: String,
  val consecutiveFailedAttempts: String,
  val titleTemplate: String,
  val bodyTemplate: String,
)

enum class NotificationRuleStatus {
  NeverFired,
  LastAttemptFailed,
  LastFired,
}
