package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.request.apprisenotificationsettings.UpdateAppriseNotificationSettingsRequest
import dev.halim.shelfdroid.core.data.GenericState
import java.net.URI

data class AppriseNotificationSettingsUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: AppriseNotificationSettingsApiState = AppriseNotificationSettingsApiState.Idle,
  val canAccess: Boolean = true,
  val savedSettings: AppriseGlobalSettingsForm = AppriseGlobalSettingsForm(),
  val draftSettings: AppriseGlobalSettingsForm = AppriseGlobalSettingsForm(),
  val notificationRules: List<NotificationRuleUi> = emptyList(),
) {
  val hasChanges: Boolean
    get() = savedSettings != draftSettings

  val validation: AppriseGlobalSettingsValidation
    get() = validateAppriseGlobalSettings(draftSettings)

  val isSavingSettings: Boolean
    get() = apiState is AppriseNotificationSettingsApiState.Loading

  val canSave: Boolean
    get() = canAccess && hasChanges && validation.isValid && !isSavingSettings
}

data class AppriseGlobalSettingsForm(
  val appriseApiUrl: String = "",
  val maxNotificationQueue: String = "",
  val maxFailedAttempts: String = "",
)

data class AppriseGlobalSettingsValidation(
  val apiUrlError: AppriseGlobalSettingsFieldError? = null,
  val maxNotificationQueueError: AppriseGlobalSettingsFieldError? = null,
  val maxFailedAttemptsError: AppriseGlobalSettingsFieldError? = null,
  val hasNotifyEndpointWarning: Boolean = false,
) {
  val isValid: Boolean
    get() =
      apiUrlError == null &&
        maxNotificationQueueError == null &&
        maxFailedAttemptsError == null
}

enum class AppriseGlobalSettingsFieldError {
  Required,
  InvalidUrl,
  PositiveInteger,
}

sealed interface AppriseNotificationSettingsApiState {
  data object Idle : AppriseNotificationSettingsApiState

  data object Loading : AppriseNotificationSettingsApiState

  data object Success : AppriseNotificationSettingsApiState

  data class Failure(val message: String?) : AppriseNotificationSettingsApiState
}

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

internal fun validateAppriseGlobalSettings(
  form: AppriseGlobalSettingsForm
): AppriseGlobalSettingsValidation {
  val apiUrl = form.appriseApiUrl.trim()
  val maxNotificationQueue = form.maxNotificationQueue.trim()
  val maxFailedAttempts = form.maxFailedAttempts.trim()
  val parsedUri = apiUrl.toAbsoluteUriOrNull()
  val apiUrlError =
    when {
      apiUrl.isEmpty() -> AppriseGlobalSettingsFieldError.Required
      parsedUri == null -> AppriseGlobalSettingsFieldError.InvalidUrl
      else -> null
    }

  return AppriseGlobalSettingsValidation(
    apiUrlError = apiUrlError,
    maxNotificationQueueError = positiveIntegerError(maxNotificationQueue),
    maxFailedAttemptsError = positiveIntegerError(maxFailedAttempts),
    hasNotifyEndpointWarning =
      apiUrlError == null && parsedUri?.path.orEmpty().trimEnd('/').endsWith("/notify") != true,
  )
}

internal fun AppriseGlobalSettingsForm.toRequest(): UpdateAppriseNotificationSettingsRequest =
  UpdateAppriseNotificationSettingsRequest(
    appriseApiUrl = appriseApiUrl.trim(),
    maxNotificationQueue = maxNotificationQueue.trim().toInt(),
    maxFailedAttempts = maxFailedAttempts.trim().toInt(),
  )

private fun positiveIntegerError(value: String): AppriseGlobalSettingsFieldError? =
  when {
    value.isEmpty() -> AppriseGlobalSettingsFieldError.Required
    value.toIntOrNull()?.takeIf { it > 0 } == null -> AppriseGlobalSettingsFieldError.PositiveInteger
    else -> null
  }

private fun String.toAbsoluteUriOrNull(): URI? =
  runCatching { URI(this) }
    .getOrNull()
    ?.takeIf { uri ->
      uri.isAbsolute &&
        uri.scheme != null &&
        !uri.host.isNullOrBlank()
    }
