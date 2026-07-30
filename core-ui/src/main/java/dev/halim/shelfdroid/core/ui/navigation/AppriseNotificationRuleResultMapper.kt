package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleStatus
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.navigation.AppriseNotificationRuleChangedNavResult
import dev.halim.shelfdroid.core.navigation.AppriseNotificationRuleNavResult

internal fun NotificationRuleUi.toNavResult() =
  AppriseNotificationRuleNavResult(
    id = id,
    libraryId = form.libraryId,
    eventName = eventName,
    urls = form.urls,
    titleTemplate = titleTemplate,
    bodyTemplate = bodyTemplate,
    enabled = enabled,
    type = form.type,
    destinationSummary = destinationSummary,
    statusName = status.name,
    statusValue = statusValue,
    consecutiveFailedAttempts = consecutiveFailedAttempts,
  )

internal fun AppriseNotificationRuleChangedNavResult.applyTo(
  uiState: AppriseNotificationSettingsUiState
): AppriseNotificationSettingsUiState =
  uiState.copy(
    state = GenericState.Success,
    apiState = AppriseNotificationSettingsApiState.Idle,
    notificationRules = notificationRules.map { it.toUi() },
  )

private fun AppriseNotificationRuleNavResult.toUi() =
  NotificationRuleUi(
    id = id,
    eventName = eventName,
    enabled = enabled,
    destinationSummary = destinationSummary,
    status = NotificationRuleStatus.valueOf(statusName),
    statusValue = statusValue,
    consecutiveFailedAttempts = consecutiveFailedAttempts,
    titleTemplate = titleTemplate,
    bodyTemplate = bodyTemplate,
    form =
      NotificationRuleForm(
        id = id,
        libraryId = libraryId,
        eventName = eventName,
        urls = urls,
        titleTemplate = titleTemplate,
        bodyTemplate = bodyTemplate,
        enabled = enabled,
        type = type,
      ),
  )
