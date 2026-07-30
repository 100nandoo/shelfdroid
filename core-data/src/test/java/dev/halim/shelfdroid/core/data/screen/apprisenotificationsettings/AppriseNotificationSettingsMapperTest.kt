package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationData
import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationEvent
import dev.halim.core.network.response.apprisenotificationsettings.AppriseNotificationEventDefaults
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

  @Test
  fun map_exposesNotificationEventsAndPreservesHiddenRuleFieldsForEditing() {
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
                      libraryId = "library-1",
                      eventName = "onTest",
                      urls = listOf("discord://alerts"),
                      titleTemplate = "Title",
                      bodyTemplate = "Body",
                      enabled = false,
                      type = "library",
                    )
                  )
              ),
            data =
              AppriseNotificationData(
                events =
                  listOf(
                    AppriseNotificationEvent(
                      name = "onTest",
                      description = "Send a test notification",
                      variables = listOf("title", "body"),
                      defaults =
                        AppriseNotificationEventDefaults(
                          title = "Default title",
                          body = "Default body",
                        ),
                    )
                  )
              ),
          ),
        formatDateTime = { error("formatDateTime should not be called for never-fired rules") },
      )

    val event = uiState.notificationEvents.single()
    assertEquals("onTest", event.name)
    assertEquals("Send a test notification", event.description)
    assertEquals(listOf("title", "body"), event.variables)
    assertEquals("Default title", event.defaultTitleTemplate)
    assertEquals("Default body", event.defaultBodyTemplate)

    val form = uiState.notificationRules.single().form
    assertEquals("rule-1", form.id)
    assertEquals("library-1", form.libraryId)
    assertEquals("onTest", form.eventName)
    assertEquals(listOf("discord://alerts"), form.urls)
    assertEquals("Title", form.titleTemplate)
    assertEquals("Body", form.bodyTemplate)
    assertEquals(false, form.enabled)
    assertEquals("library", form.type)
  }

  @Test
  fun applySettings_updatesRulesAndGlobalSettingsWithoutDiscardingLoadedEvents() {
    val existingState =
      AppriseNotificationSettingsMapper.map(
        response =
          AppriseNotificationSettingsResponse(
            settings =
              AppriseNotificationSettings(
                appriseApiUrl = "https://old.example.com/notify",
                maxNotificationQueue = 2,
                maxFailedAttempts = 1,
              ),
            data =
              AppriseNotificationData(
                events =
                  listOf(
                    AppriseNotificationEvent(
                      name = "onPodcastEpisodeDownloaded",
                      description = "Episode downloaded",
                      variables = listOf("episodeTitle"),
                      defaults =
                        AppriseNotificationEventDefaults(
                          title = "Default title",
                          body = "Default body",
                        ),
                    )
                  )
              ),
          ),
        formatDateTime = { "formatted-$it" },
      )

    val updatedState =
      AppriseNotificationSettingsMapper.applySettings(
        uiState = existingState,
        settings =
          AppriseNotificationSettings(
            appriseApiUrl = "https://new.example.com/notify",
            maxNotificationQueue = 5,
            maxFailedAttempts = 3,
            notifications =
              listOf(
                AppriseNotificationRule(
                  id = "rule-9",
                  eventName = "onPodcastEpisodeDownloaded",
                  urls = listOf("telegram://download"),
                  titleTemplate = "New {{podcastTitle}} Episode!",
                  bodyTemplate = "{{episodeTitle}} has been added.",
                  enabled = true,
                  lastFiredAt = 1_752_000_000_000L,
                )
              ),
          ),
        formatDateTime = { "formatted-$it" },
      )

    assertEquals("https://new.example.com/notify", updatedState.savedSettings.appriseApiUrl)
    assertEquals("5", updatedState.savedSettings.maxNotificationQueue)
    assertEquals("3", updatedState.savedSettings.maxFailedAttempts)
    assertEquals(updatedState.savedSettings, updatedState.draftSettings)
    assertEquals(existingState.notificationEvents, updatedState.notificationEvents)
    assertEquals(1, updatedState.notificationRules.size)
    assertEquals("rule-9", updatedState.notificationRules.single().id)
    assertEquals("formatted-1752000000000", updatedState.notificationRules.single().statusValue)
  }
}
