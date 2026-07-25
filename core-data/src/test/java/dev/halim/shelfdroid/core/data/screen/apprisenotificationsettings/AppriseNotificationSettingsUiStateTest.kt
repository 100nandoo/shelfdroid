package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AppriseNotificationSettingsUiStateTest {

  @Test
  fun ruleValidation_rejectsBlankDestinationUrls() {
    val validation =
      validateNotificationRule(
        NotificationRuleForm(
          eventName = "onTest",
          urls = listOf("https://example.com", "  "),
        )
      )

    assertFalse(validation.isValid)
    assertTrue(validation.hasBlankDestinationUrl)
  }

  @Test
  fun eventChange_replacesTemplatesWithUpstreamDefaults() {
    val event =
      NotificationEventUi(
        name = "onBackupFailed",
        defaultTitleTemplate = "Backup Failed",
        defaultBodyTemplate = "{{errorMsg}}",
      )

    val updated =
      NotificationRuleForm(
          eventName = "onTest",
          titleTemplate = "Custom title",
          bodyTemplate = "Custom body",
        )
        .withEvent(event)

    assertEquals("onBackupFailed", updated.eventName)
    assertEquals("Backup Failed", updated.titleTemplate)
    assertEquals("{{errorMsg}}", updated.bodyTemplate)
  }

  @Test
  fun validation_acceptsAbsoluteNotifyEndpointAndPositiveIntegers() {
    val validation =
      validateAppriseGlobalSettings(
        AppriseGlobalSettingsForm(
          appriseApiUrl = "https://apprise.example.com/notify",
          maxNotificationQueue = "5",
          maxFailedAttempts = "3",
        )
      )

    assertTrue(validation.isValid)
    assertNull(validation.apiUrlError)
    assertNull(validation.maxNotificationQueueError)
    assertNull(validation.maxFailedAttemptsError)
    assertFalse(validation.hasNotifyEndpointWarning)
  }

  @Test
  fun validation_rejectsMalformedOrIncompleteInput() {
    val validation =
      validateAppriseGlobalSettings(
        AppriseGlobalSettingsForm(
          appriseApiUrl = "apprise.example.com",
          maxNotificationQueue = "0",
          maxFailedAttempts = "",
        )
      )

    assertFalse(validation.isValid)
    assertEquals(AppriseGlobalSettingsFieldError.InvalidUrl, validation.apiUrlError)
    assertEquals(
      AppriseGlobalSettingsFieldError.PositiveInteger,
      validation.maxNotificationQueueError,
    )
    assertEquals(AppriseGlobalSettingsFieldError.Required, validation.maxFailedAttemptsError)
  }

  @Test
  fun validation_warnsForNonNotifyEndpointWithoutBlockingSave() {
    val validation =
      validateAppriseGlobalSettings(
        AppriseGlobalSettingsForm(
          appriseApiUrl = "https://apprise.example.com/api",
          maxNotificationQueue = "2",
          maxFailedAttempts = "1",
        )
      )

    assertTrue(validation.isValid)
    assertTrue(validation.hasNotifyEndpointWarning)
  }

  @Test
  fun validation_doesNotWarnForNotifyEndpointWithTrailingSlash() {
    val validation =
      validateAppriseGlobalSettings(
        AppriseGlobalSettingsForm(
          appriseApiUrl = "https://apprise.example.com/notify/",
          maxNotificationQueue = "2",
          maxFailedAttempts = "1",
        )
      )

    assertTrue(validation.isValid)
    assertFalse(validation.hasNotifyEndpointWarning)
  }

  @Test
  fun canSave_requiresChangesValidDraftAndIdleMutationState() {
    val saved =
      AppriseGlobalSettingsForm(
        appriseApiUrl = "https://apprise.example.com/notify",
        maxNotificationQueue = "5",
        maxFailedAttempts = "3",
      )
    val dirtyDraft =
      saved.copy(
        appriseApiUrl = "https://apprise.example.com/api",
        maxNotificationQueue = "6",
      )

    val cleanState =
      AppriseNotificationSettingsUiState(
        savedSettings = saved,
        draftSettings = saved,
      )
    val dirtyState =
      AppriseNotificationSettingsUiState(
        savedSettings = saved,
        draftSettings = dirtyDraft,
      )
    val savingState =
      dirtyState.copy(
        apiState =
          AppriseNotificationSettingsApiState.Loading(
            AppriseNotificationSettingsMutationTarget.GlobalSettings
          )
      )
    val invalidState = dirtyState.copy(draftSettings = dirtyDraft.copy(maxNotificationQueue = "-1"))

    assertFalse(cleanState.canSave)
    assertTrue(dirtyState.canSave)
    assertFalse(savingState.canSave)
    assertFalse(invalidState.canSave)
  }

  @Test
  fun savingFlags_distinguishGlobalSettingsFromRuleMutations() {
    val settingsSavingState =
      AppriseNotificationSettingsUiState(
        apiState =
          AppriseNotificationSettingsApiState.Loading(
            AppriseNotificationSettingsMutationTarget.GlobalSettings
          )
      )
    val ruleSavingState =
      AppriseNotificationSettingsUiState(
        apiState =
          AppriseNotificationSettingsApiState.Loading(
            AppriseNotificationSettingsMutationTarget.NotificationRule
          )
      )
    val ruleTestSavingState =
      AppriseNotificationSettingsUiState(
        apiState =
          AppriseNotificationSettingsApiState.Loading(
            AppriseNotificationSettingsMutationTarget.NotificationRuleTest
          )
      )

    assertTrue(settingsSavingState.isSavingSettings)
    assertFalse(settingsSavingState.isMutatingRule)
    assertFalse(ruleSavingState.isSavingSettings)
    assertTrue(ruleSavingState.isMutatingRule)
    assertFalse(ruleTestSavingState.isSavingSettings)
    assertTrue(ruleTestSavingState.isMutatingRule)
  }

  @Test
  fun notificationRuleTestFailureReason_mapsKnownHttpCodes() {
    assertEquals(
      AppriseNotificationSettingsFailureReason.AppriseNotConfigured,
      notificationRuleTestFailureReason(httpException(400)),
    )
    assertEquals(
      AppriseNotificationSettingsFailureReason.DeliveryFailed,
      notificationRuleTestFailureReason(httpException(500)),
    )
  }

  @Test
  fun notificationRuleTestFailureReason_ignoresUnknownFailures() {
    assertNull(notificationRuleTestFailureReason(httpException(404)))
    assertNull(notificationRuleTestFailureReason(IllegalStateException("boom")))
  }

  private fun httpException(code: Int): HttpException =
    HttpException(
      Response.error<Unit>(
        code,
        "".toResponseBody("text/plain".toMediaType()),
      )
    )
}
