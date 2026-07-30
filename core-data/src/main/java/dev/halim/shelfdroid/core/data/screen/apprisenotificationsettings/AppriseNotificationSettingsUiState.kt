package dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings

import dev.halim.core.network.request.apprisenotificationsettings.AppriseNotificationRuleRequest
import dev.halim.core.network.request.apprisenotificationsettings.NotificationRuleEnabledRequest
import dev.halim.core.network.request.apprisenotificationsettings.UpdateAppriseNotificationSettingsRequest
import dev.halim.shelfdroid.core.data.GenericState
import java.net.URI
import retrofit2.HttpException

data class AppriseNotificationSettingsUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: AppriseNotificationSettingsApiState = AppriseNotificationSettingsApiState.Idle,
  val savedSettings: AppriseGlobalSettingsForm = AppriseGlobalSettingsForm(),
  val draftSettings: AppriseGlobalSettingsForm = AppriseGlobalSettingsForm(),
  val notificationRules: List<NotificationRuleUi> = emptyList(),
  val notificationEvents: List<NotificationEventUi> = emptyList(),
) {
  val hasChanges: Boolean
    get() = savedSettings != draftSettings

  val validation: AppriseGlobalSettingsValidation
    get() = validateAppriseGlobalSettings(draftSettings)

  val isSavingSettings: Boolean
    get() =
      apiState is AppriseNotificationSettingsApiState.Loading &&
        apiState.target == AppriseNotificationSettingsMutationTarget.GlobalSettings

  val isMutatingRule: Boolean
    get() =
      apiState is AppriseNotificationSettingsApiState.Loading &&
        apiState.target != AppriseNotificationSettingsMutationTarget.GlobalSettings

  val canSave: Boolean
    get() = hasChanges && validation.isValid && !isSavingSettings
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
      apiUrlError == null && maxNotificationQueueError == null && maxFailedAttemptsError == null
}

enum class AppriseGlobalSettingsFieldError {
  Required,
  InvalidUrl,
  PositiveInteger,
}

enum class AppriseNotificationSettingsMutationTarget {
  GlobalSettings,
  NotificationRule,
  NotificationRuleEnable,
  NotificationRuleDelete,
  NotificationRuleTest,
}

enum class AppriseNotificationSettingsFailureReason {
  AppriseNotConfigured,
  DeliveryFailed,
}

sealed interface AppriseNotificationSettingsApiState {
  data object Idle : AppriseNotificationSettingsApiState

  data class Loading(val target: AppriseNotificationSettingsMutationTarget) :
    AppriseNotificationSettingsApiState

  data class Success(val target: AppriseNotificationSettingsMutationTarget) :
    AppriseNotificationSettingsApiState

  data class Failure(
    val target: AppriseNotificationSettingsMutationTarget,
    val message: String?,
    val reason: AppriseNotificationSettingsFailureReason? = null,
  ) : AppriseNotificationSettingsApiState
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
  val form: NotificationRuleForm = NotificationRuleForm(),
)

data class NotificationEventUi(
  val name: String,
  val description: String = "",
  val variables: List<String> = emptyList(),
  val defaultTitleTemplate: String = "",
  val defaultBodyTemplate: String = "",
)

data class NotificationRuleForm(
  val id: String? = null,
  val libraryId: String? = null,
  val eventName: String = "onTest",
  val urls: List<String> = emptyList(),
  val titleTemplate: String = "",
  val bodyTemplate: String = "",
  val enabled: Boolean = true,
  val type: String? = null,
)

data class NotificationRuleValidation(val hasBlankDestinationUrl: Boolean) {
  val isValid: Boolean
    get() = !hasBlankDestinationUrl
}

fun validateNotificationRule(form: NotificationRuleForm) =
  NotificationRuleValidation(form.urls.isEmpty() || form.urls.any { it.isBlank() })

fun NotificationRuleForm.withEvent(event: NotificationEventUi) =
  copy(
    eventName = event.name,
    titleTemplate = event.defaultTitleTemplate,
    bodyTemplate = event.defaultBodyTemplate,
  )

internal fun NotificationRuleForm.toRequest() =
  AppriseNotificationRuleRequest(
    id = id,
    libraryId = libraryId,
    eventName = eventName,
    urls = urls.map(String::trim),
    titleTemplate = titleTemplate,
    bodyTemplate = bodyTemplate,
    enabled = enabled,
    type = type,
  )

internal fun NotificationRuleUi.toEnableRequest() =
  NotificationRuleEnabledRequest(
    id = id,
    enabled = true,
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
    value.toIntOrNull()?.takeIf { it > 0 } == null ->
      AppriseGlobalSettingsFieldError.PositiveInteger

    else -> null
  }

private fun String.toAbsoluteUriOrNull(): URI? =
  runCatching { URI(this) }
    .getOrNull()
    ?.takeIf { uri ->
      uri.isAbsolute && uri.scheme != null && !uri.host.isNullOrBlank()
    }

internal fun notificationRuleTestFailureReason(
  error: Throwable
): AppriseNotificationSettingsFailureReason? =
  when ((error as? HttpException)?.code()) {
    400 -> AppriseNotificationSettingsFailureReason.AppriseNotConfigured
    500 -> AppriseNotificationSettingsFailureReason.DeliveryFailed
    else -> null
  }
