package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppriseNotificationSettingsUiStateTest {

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
      dirtyState.copy(apiState = AppriseNotificationSettingsApiState.Loading)
    val invalidState =
      dirtyState.copy(
        draftSettings = dirtyDraft.copy(maxNotificationQueue = "-1"),
      )

    assertFalse(cleanState.canSave)
    assertTrue(dirtyState.canSave)
    assertFalse(savingState.canSave)
    assertFalse(invalidState.canSave)
  }
}
