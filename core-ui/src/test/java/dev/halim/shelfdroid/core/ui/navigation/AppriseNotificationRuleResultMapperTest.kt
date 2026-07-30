package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMutationTarget
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationEventUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleStatus
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.navigation.AppriseNotificationRuleChangedNavResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppriseNotificationRuleResultMapperTest {

  @Test
  fun navResultRoundTrip_updatesRulesWithoutDiscardingParentState() {
    val savedSettings =
      AppriseGlobalSettingsForm(
        appriseApiUrl = "https://apprise.example.com/notify",
        maxNotificationQueue = "5",
        maxFailedAttempts = "3",
      )
    val existingState =
      AppriseNotificationSettingsUiState(
        state = GenericState.Success,
        apiState =
          AppriseNotificationSettingsApiState.Loading(
            AppriseNotificationSettingsMutationTarget.NotificationRuleTest
          ),
        savedSettings = savedSettings,
        draftSettings = savedSettings,
        notificationEvents =
          listOf(
            NotificationEventUi(
              name = "onPodcastEpisodeDownloaded",
              description = "Episode downloaded",
            )
          ),
      )
    val rule =
      NotificationRuleUi(
        id = "rule-1",
        eventName = "onPodcastEpisodeDownloaded",
        enabled = true,
        destinationSummary = "telegram://download",
        status = NotificationRuleStatus.LastAttemptFailed,
        statusValue = "29 July 2026 9:15AM",
        consecutiveFailedAttempts = "2",
        titleTemplate = "New {{podcastTitle}} Episode!",
        bodyTemplate = "{{episodeTitle}} has been added.",
        form =
          NotificationRuleForm(
            id = "rule-1",
            libraryId = "library-1",
            eventName = "onPodcastEpisodeDownloaded",
            urls = listOf("telegram://download"),
            titleTemplate = "New {{podcastTitle}} Episode!",
            bodyTemplate = "{{episodeTitle}} has been added.",
            enabled = true,
            type = "library",
          ),
      )

    val updatedState =
      AppriseNotificationRuleChangedNavResult(notificationRules = listOf(rule.toNavResult()))
        .applyTo(existingState)

    assertEquals(GenericState.Success, updatedState.state)
    assertEquals(AppriseNotificationSettingsApiState.Idle, updatedState.apiState)
    assertEquals(savedSettings, updatedState.savedSettings)
    assertEquals(savedSettings, updatedState.draftSettings)
    assertEquals(existingState.notificationEvents, updatedState.notificationEvents)
    assertEquals(1, updatedState.notificationRules.size)
    assertEquals(rule.id, updatedState.notificationRules.single().id)
    assertEquals(rule.status, updatedState.notificationRules.single().status)
    assertEquals(rule.statusValue, updatedState.notificationRules.single().statusValue)
    assertEquals(rule.form.libraryId, updatedState.notificationRules.single().form.libraryId)
    assertEquals(rule.form.type, updatedState.notificationRules.single().form.type)
    assertEquals(rule.form.urls, updatedState.notificationRules.single().form.urls)
    assertTrue(updatedState.notificationRules.single().enabled)
  }
}
