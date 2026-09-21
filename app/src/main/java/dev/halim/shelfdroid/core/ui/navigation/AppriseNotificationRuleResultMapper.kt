package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
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
