package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationRule
import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationSettings
import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationSettingsResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppriseNotificationSettingsMapperTest {

  @Test
  fun map_preservesGlobalSettingsAndHandlesEmptyNotificationRules() {
    val uiState =
      AppriseNotificationSettingsMapper.map(
        response =
          AppriseNotificationSettingsResponse(
            settings =
              AppriseNotificationSettings(
                appriseApiUrl = "https://apprise.example.com/notify",
                maxNotificationQueue = 5,
                maxFailedAttempts = 3,
              )
          ),
        formatDateTime = { error("formatDateTime should not be called for empty rules") },
      )

    assertEquals("https://apprise.example.com/notify", uiState.savedSettings.appriseApiUrl)
    assertEquals("5", uiState.savedSettings.maxNotificationQueue)
    assertEquals("3", uiState.savedSettings.maxFailedAttempts)
    assertEquals(uiState.savedSettings, uiState.draftSettings)
    assertTrue(uiState.notificationRules.isEmpty())
  }

  @Test
  fun map_formatsNotificationRuleStatusAndDestinationSummary() {
    val uiState =
      AppriseNotificationSettingsMapper.map(
        response =
          AppriseNotificationSettingsResponse(
            settings =
              AppriseNotificationSettings(
                notifications =
                  listOf(
                    AppriseNotificationRule(
                      id = "rule-1",
                      eventName = "onBackupFailed",
                      urls = listOf("discord://alerts", "mailto://ops@example.com"),
                      titleTemplate = "Backup failed",
                      bodyTemplate = "The latest backup failed",
                      enabled = false,
                      lastFiredAt = 1_752_000_000_000L,
                      lastAttemptFailed = true,
                      numConsecutiveFailedAttempts = 4,
                    ),
                    AppriseNotificationRule(
                      id = "rule-2",
                      eventName = "onRSSFeedDisabled",
                      urls = listOf("mailto://reader@example.com"),
                      enabled = true,
                      lastFiredAt = null,
                      lastAttemptFailed = false,
                      numConsecutiveFailedAttempts = 0,
                    ),
                  )
              )
          ),
        formatDateTime = { "formatted-$it" },
      )

    assertEquals(2, uiState.notificationRules.size)

    val failedRule = uiState.notificationRules.first()
    assertEquals("onBackupFailed", failedRule.eventName)
    assertEquals("discord://alerts\nmailto://ops@example.com", failedRule.destinationSummary)
    assertEquals(false, failedRule.enabled)
    assertEquals(NotificationRuleStatus.LastAttemptFailed, failedRule.status)
    assertEquals("formatted-1752000000000", failedRule.statusValue)
    assertEquals("4", failedRule.consecutiveFailedAttempts)

    val neverFiredRule = uiState.notificationRules.last()
    assertEquals(true, neverFiredRule.enabled)
    assertEquals(NotificationRuleStatus.NeverFired, neverFiredRule.status)
    assertEquals("", neverFiredRule.statusValue)
    assertEquals("0", neverFiredRule.consecutiveFailedAttempts)
  }
}
