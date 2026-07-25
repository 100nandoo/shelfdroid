package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions as ComposeKeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsFieldError
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsValidation
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleStatus
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun AppriseNotificationSettingsScreen(
  snackbarHostState: SnackbarHostState,
  viewModel: AppriseNotificationSettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HandleAppriseNotificationSettingsSnackbar(uiState, snackbarHostState)
  AppriseNotificationSettingsContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun AppriseNotificationSettingsContent(
  uiState: AppriseNotificationSettingsUiState = AppriseNotificationSettingsUiState(),
  onEvent: (AppriseNotificationSettingsEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize()) {
    VisibilityDown(
      uiState.state is GenericState.Loading || uiState.apiState is AppriseNotificationSettingsApiState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    val state = uiState.state
    if (state is GenericState.Failure) {
      GenericMessageScreen(state.errorMessage ?: "")
      return
    }

    if (state != GenericState.Success) {
      return
    }

    if (!uiState.canAccess) {
      GenericMessageScreen(stringResource(R.string.apprise_notification_settings_admin_only))
      return
    }

    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      TextTitleMedium(text = stringResource(R.string.apprise_notification_settings))
      TextLabelSmall(
        modifier = Modifier.padding(top = 4.dp),
        text = stringResource(R.string.apprise_notification_settings_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(16.dp))
      GlobalSettingsSection(uiState = uiState, onEvent = onEvent)
      Spacer(modifier = Modifier.height(24.dp))
      NotificationRulesSection(uiState.notificationRules)
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun GlobalSettingsSection(
  uiState: AppriseNotificationSettingsUiState,
  onEvent: (AppriseNotificationSettingsEvent) -> Unit,
) {
  val draft = uiState.draftSettings
  val validation = uiState.validation

  MyOutlinedTextField(
    value = draft.appriseApiUrl,
    onValueChange = { value ->
      onEvent(AppriseNotificationSettingsEvent.UpdateDraftSettings { it.copy(appriseApiUrl = value) })
    },
    label = stringResource(R.string.apprise_api_url),
    placeholder = stringResource(R.string.apprise_api_url_placeholder),
    supportingText = apiUrlSupportingText(validation),
    isError = validation.apiUrlError != null,
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings,
  )
  Spacer(modifier = Modifier.height(12.dp))
  MyOutlinedTextField(
    value = draft.maxNotificationQueue,
    onValueChange = { value ->
      onEvent(
        AppriseNotificationSettingsEvent.UpdateDraftSettings {
          it.copy(maxNotificationQueue = value)
        }
      )
    },
    label = stringResource(R.string.max_queue_size_for_notification_events),
    supportingText = integerSupportingText(validation.maxNotificationQueueError),
    isError = validation.maxNotificationQueueError != null,
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings,
  )
  Spacer(modifier = Modifier.height(12.dp))
  MyOutlinedTextField(
    value = draft.maxFailedAttempts,
    onValueChange = { value ->
      onEvent(
        AppriseNotificationSettingsEvent.UpdateDraftSettings {
          it.copy(maxFailedAttempts = value)
        }
      )
    },
    label = stringResource(R.string.max_failed_attempts),
    supportingText = integerSupportingText(validation.maxFailedAttemptsError),
    isError = validation.maxFailedAttemptsError != null,
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    enabled = !uiState.isSavingSettings,
  )
  Spacer(modifier = Modifier.height(12.dp))
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    Button(
      enabled = uiState.canSave,
      onClick = { onEvent(AppriseNotificationSettingsEvent.SaveSettings) },
    ) {
      Text(stringResource(R.string.save))
    }
  }
}

@Composable
private fun NotificationRulesSection(notificationRules: List<NotificationRuleUi>) {
  TextTitleMedium(text = stringResource(R.string.notification_rules))
  Spacer(modifier = Modifier.height(8.dp))

  if (notificationRules.isEmpty()) {
    TextBodyMedium(
      text = stringResource(R.string.empty_type, stringResource(R.string.notification_rules)),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }

  Column {
    notificationRules.forEachIndexed { index, rule ->
      if (index > 0) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
      }
      NotificationRuleCard(rule = rule)
    }
  }
}

@Composable
private fun NotificationRuleCard(rule: NotificationRuleUi) {
  TextTitleSmall(text = rule.eventName)
  TextLabelSmall(
    text = stringResource(if (rule.enabled) R.string.enabled else R.string.disabled),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Spacer(modifier = Modifier.height(12.dp))
  SettingValueRow(label = stringResource(R.string.destinations), value = rule.destinationSummary)
  Spacer(modifier = Modifier.height(8.dp))
  SettingValueRow(label = statusLabel(rule.status), value = statusValue(rule))
  Spacer(modifier = Modifier.height(8.dp))
  SettingValueRow(
    label = stringResource(R.string.consecutive_failed_attempts),
    value = rule.consecutiveFailedAttempts,
  )
  if (rule.titleTemplate.isNotBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    SettingValueRow(label = stringResource(R.string.title_template), value = rule.titleTemplate)
  }
  if (rule.bodyTemplate.isNotBlank()) {
    Spacer(modifier = Modifier.height(8.dp))
    SettingValueRow(label = stringResource(R.string.body_template), value = rule.bodyTemplate)
  }
}

@Composable
private fun SettingValueRow(label: String, value: String) {
  TextLabelSmall(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
  TextBodyMedium(text = value.ifBlank { "—" })
}

@Composable
private fun statusLabel(status: NotificationRuleStatus): String =
  stringResource(
    when (status) {
      NotificationRuleStatus.NeverFired -> R.string.status
      NotificationRuleStatus.LastAttemptFailed -> R.string.last_attempt_failed
      NotificationRuleStatus.LastFired -> R.string.last_fired
    }
  )

@Composable
private fun statusValue(rule: NotificationRuleUi): String =
  if (rule.status == NotificationRuleStatus.NeverFired) stringResource(R.string.never_fired)
  else rule.statusValue

@Composable
private fun HandleAppriseNotificationSettingsSnackbar(
  uiState: AppriseNotificationSettingsUiState,
  snackbarHostState: SnackbarHostState,
) {
  val successMessage = stringResource(R.string.settings_saved)
  val errorMessage = stringResource(R.string.settings_save_failed)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      AppriseNotificationSettingsApiState.Success ->
        snackbarHostState.showSuccessSnackbar(successMessage)
      is AppriseNotificationSettingsApiState.Failure ->
        snackbarHostState.showErrorSnackbar(state.message ?: errorMessage)
      else -> Unit
    }
  }
}

@Composable
private fun apiUrlSupportingText(validation: AppriseGlobalSettingsValidation): String? =
  when (validation.apiUrlError) {
    AppriseGlobalSettingsFieldError.Required -> stringResource(R.string.apprise_api_url_required)
    AppriseGlobalSettingsFieldError.InvalidUrl -> stringResource(R.string.enter_a_valid_absolute_url)
    null ->
      if (validation.hasNotifyEndpointWarning) {
        stringResource(R.string.apprise_api_url_notify_warning)
      } else {
        null
      }
    else -> null
  }

@Composable
private fun integerSupportingText(error: AppriseGlobalSettingsFieldError?): String? =
  when (error) {
    AppriseGlobalSettingsFieldError.Required -> stringResource(R.string.field_required)
    AppriseGlobalSettingsFieldError.PositiveInteger ->
      stringResource(R.string.enter_a_positive_integer)
    else -> null
  }

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          savedSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "https://apprise.example.com/notify",
              maxNotificationQueue = "5",
              maxFailedAttempts = "3",
            ),
          draftSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "https://apprise.example.com/notify",
              maxNotificationQueue = "5",
              maxFailedAttempts = "3",
            ),
          notificationRules =
            listOf(
              NotificationRuleUi(
                id = "rule-1",
                eventName = "onBackupFailed",
                enabled = true,
                destinationSummary = "discord://alerts\nmailto://ops@example.com",
                status = NotificationRuleStatus.LastFired,
                statusValue = "25 July 2026 3:45PM",
                consecutiveFailedAttempts = "0",
                titleTemplate = "Backup failed",
                bodyTemplate = "The latest backup failed on the Audiobookshelf server.",
              ),
              NotificationRuleUi(
                id = "rule-2",
                eventName = "onRSSFeedDisabled",
                enabled = false,
                destinationSummary = "mailto://reader@example.com",
                status = NotificationRuleStatus.NeverFired,
                statusValue = "",
                consecutiveFailedAttempts = "2",
                titleTemplate = "",
                bodyTemplate = "",
              ),
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsEmptyPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          savedSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "",
              maxNotificationQueue = "10",
              maxFailedAttempts = "5",
            ),
          draftSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "",
              maxNotificationQueue = "10",
              maxFailedAttempts = "5",
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsValidationPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          savedSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "https://apprise.example.com/notify",
              maxNotificationQueue = "5",
              maxFailedAttempts = "3",
            ),
          draftSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "https://apprise.example.com/notify/",
              maxNotificationQueue = "0",
              maxFailedAttempts = "",
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsSavingPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          apiState = AppriseNotificationSettingsApiState.Loading,
          savedSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "https://apprise.example.com/notify",
              maxNotificationQueue = "5",
              maxFailedAttempts = "3",
            ),
          draftSettings =
            AppriseGlobalSettingsForm(
              appriseApiUrl = "https://apprise.example.com/notify/",
              maxNotificationQueue = "6",
              maxFailedAttempts = "3",
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsLoadingPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState = AppriseNotificationSettingsUiState(state = GenericState.Loading)
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsErrorPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Failure("Unable to load Apprise notification settings."),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsAdminOnlyPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          canAccess = false,
        )
    )
  }
}
