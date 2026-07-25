package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationRule
import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationSettingsResponse

internal object AppriseNotificationSettingsMapper {
  fun map(
    response: AppriseNotificationSettingsResponse,
    formatDateTime: (Long) -> String,
  ): AppriseNotificationSettingsUiState {
    val settings = response.settings
    return AppriseNotificationSettingsUiState(
      settings =
        AppriseGlobalSettingsUi(
          appriseApiUrl = settings.appriseApiUrl.orEmpty(),
          maxNotificationQueue = settings.maxNotificationQueue.toString(),
          maxFailedAttempts = settings.maxFailedAttempts.toString(),
        ),
      notificationRules = settings.notifications.map { rule -> mapRule(rule, formatDateTime) },
    )
  }

  private fun mapRule(
    rule: AppriseNotificationRule,
    formatDateTime: (Long) -> String,
  ): NotificationRuleUi {
    val statusValue =
      rule.lastFiredAt?.let(formatDateTime)
        ?: ""

    return NotificationRuleUi(
      id = rule.id,
      eventName = rule.eventName,
      enabled = rule.enabled,
      destinationSummary = rule.urls.joinToString(separator = "\n"),
      status =
        when {
          rule.lastFiredAt == null -> NotificationRuleStatus.NeverFired
          rule.lastAttemptFailed -> NotificationRuleStatus.LastAttemptFailed
          else -> NotificationRuleStatus.LastFired
        },
      statusValue = statusValue,
      consecutiveFailedAttempts = rule.numConsecutiveFailedAttempts.toString(),
      titleTemplate = rule.titleTemplate,
      bodyTemplate = rule.bodyTemplate,
    )
  }
}
