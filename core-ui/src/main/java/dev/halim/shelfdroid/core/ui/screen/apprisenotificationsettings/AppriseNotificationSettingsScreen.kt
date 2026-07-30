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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsFieldError
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsValidation
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsFailureReason
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMutationTarget
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleStatus
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.withEvent
import dev.halim.shelfdroid.core.navigation.AppriseNotificationRuleChangedNavResult
import dev.halim.shelfdroid.core.navigation.NavEditAppriseNotificationRule
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextTitleLarge
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun AppriseNotificationSettingsScreen(
  snackbarHostState: SnackbarHostState,
  collectNavResultEvent: Boolean = false,
  onCreateRule: (NavEditAppriseNotificationRule) -> Unit = {},
  onEditRule: (NavEditAppriseNotificationRule) -> Unit = {},
  viewModel: AppriseNotificationSettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  if (collectNavResultEvent) {
    ResultEffect<AppriseNotificationRuleChangedNavResult> { result ->
      viewModel.onEvent(AppriseNotificationSettingsEvent.ApplyRuleChange(result))
    }
  }
  HandleAppriseNotificationSettingsSnackbar(uiState, snackbarHostState)
  AppriseNotificationSettingsContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
    onCreateRule = onCreateRule,
    onEditRule = onEditRule,
  )
}

@Composable
private fun AppriseNotificationSettingsContent(
  uiState: AppriseNotificationSettingsUiState = AppriseNotificationSettingsUiState(),
  onEvent: (AppriseNotificationSettingsEvent) -> Unit = {},
  onCreateRule: (NavEditAppriseNotificationRule) -> Unit = {},
  onEditRule: (NavEditAppriseNotificationRule) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize()) {
    VisibilityDown(
      uiState.state is GenericState.Loading ||
        uiState.apiState is AppriseNotificationSettingsApiState.Loading
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

    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.Bottom,
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        GlobalSettingsSection(uiState = uiState, onEvent = onEvent)
        Spacer(modifier = Modifier.height(24.dp))
      }
      NotificationRulesSection(
        uiState = uiState,
        onEvent = onEvent,
        onCreateRule = onCreateRule,
        onEditRule = onEditRule,
      )
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
      onEvent(
        AppriseNotificationSettingsEvent.UpdateDraftSettings { it.copy(appriseApiUrl = value) }
      )
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
private fun NotificationRulesSection(
  uiState: AppriseNotificationSettingsUiState,
  onEvent: (AppriseNotificationSettingsEvent) -> Unit,
  onCreateRule: (NavEditAppriseNotificationRule) -> Unit,
  onEditRule: (NavEditAppriseNotificationRule) -> Unit,
) {
  var deleting by remember { mutableStateOf<NotificationRuleUi?>(null) }

  val notificationRules = uiState.notificationRules
  val notificationEvents = uiState.notificationEvents
  val apiState = uiState.apiState
  val loadingState = apiState as? AppriseNotificationSettingsApiState.Loading
  val isLoading = loadingState != null
  val isDeletingRule =
    loadingState?.target == AppriseNotificationSettingsMutationTarget.NotificationRuleDelete

  TextTitleLarge(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.notification_rules),
  )
  Spacer(modifier = Modifier.height(8.dp))

  if (notificationRules.isEmpty()) {
    TextBodyMedium(
      modifier = Modifier.padding(horizontal = 16.dp),
      text = stringResource(R.string.empty_type, stringResource(R.string.notification_rules)),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  } else {
    Column {
      notificationRules.forEach { rule ->
        HorizontalDivider()
        NotificationRuleCard(
          rule = rule,
          enabled = !isLoading,
          onEnable = { onEvent(AppriseNotificationSettingsEvent.EnableRule(rule)) },
          onTest = { onEvent(AppriseNotificationSettingsEvent.TestRule(rule)) },
          onEdit = { onEditRule(rule.form.toNavEditPayload()) },
          onDelete = { deleting = rule },
        )
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))
  TextButton(
    onClick = {
      notificationEvents.firstOrNull()?.let { event ->
        onCreateRule(NotificationRuleForm().withEvent(event).toNavEditPayload())
      }
    },
    enabled = notificationEvents.isNotEmpty() && !isLoading,
    modifier = Modifier.fillMaxWidth().padding(16.dp),
  ) {
    Text(stringResource(R.string.create))
  }
  deleting?.let { rule ->
    AlertDialog(
      onDismissRequest = { if (!isDeletingRule) deleting = null },
      title = {
        Text(
          text = stringResource(R.string.delete_notification_rule_title),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      text = { Text(stringResource(R.string.dialog_delete_text)) },
      confirmButton = {
        TextButton(
          enabled = !isDeletingRule,
          onClick = {
            deleting = null
            onEvent(AppriseNotificationSettingsEvent.DeleteRule(rule))
          },
        ) {
          Text(stringResource(R.string.delete))
        }
      },
      dismissButton = {
        TextButton(
          enabled = !isDeletingRule,
          onClick = { deleting = null },
        ) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }
}

private fun NotificationRuleForm.toNavEditPayload() =
  NavEditAppriseNotificationRule(
    id = id,
    libraryId = libraryId,
    eventName = eventName,
    urls = urls,
    titleTemplate = titleTemplate,
    bodyTemplate = bodyTemplate,
    enabled = enabled,
    type = type,
  )

@Composable
private fun HandleAppriseNotificationSettingsSnackbar(
  uiState: AppriseNotificationSettingsUiState,
  snackbarHostState: SnackbarHostState,
) {
  val settingsSuccessMessage = stringResource(R.string.settings_saved)
  val settingsErrorMessage = stringResource(R.string.settings_save_failed)
  val ruleSaveSuccessMessage = stringResource(R.string.notification_rule_saved)
  val ruleEnableSuccessMessage = stringResource(R.string.notification_rule_enabled)
  val ruleSaveErrorMessage = stringResource(R.string.notification_rule_save_failed)
  val ruleDeleteSuccessMessage = stringResource(R.string.notification_rule_deleted)
  val ruleDeleteErrorMessage = stringResource(R.string.notification_rule_delete_failed)
  val ruleTestSuccessMessage = stringResource(R.string.notification_rule_test_sent)
  val ruleTestErrorMessage = stringResource(R.string.notification_rule_test_failed)
  val ruleTestNotConfiguredMessage =
    stringResource(R.string.notification_rule_test_apprise_not_configured)
  val ruleTestDeliveryFailedMessage =
    stringResource(R.string.notification_rule_test_delivery_failed)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is AppriseNotificationSettingsApiState.Success ->
        snackbarHostState.showSuccessSnackbar(
          when (state.target) {
            AppriseNotificationSettingsMutationTarget.GlobalSettings -> settingsSuccessMessage
            AppriseNotificationSettingsMutationTarget.NotificationRule -> ruleSaveSuccessMessage
            AppriseNotificationSettingsMutationTarget.NotificationRuleEnable ->
              ruleEnableSuccessMessage
            AppriseNotificationSettingsMutationTarget.NotificationRuleDelete ->
              ruleDeleteSuccessMessage

            AppriseNotificationSettingsMutationTarget.NotificationRuleTest -> ruleTestSuccessMessage
          }
        )

      is AppriseNotificationSettingsApiState.Failure ->
        snackbarHostState.showErrorSnackbar(
          state.message.takeUnless { state.reason != null }
            ?: when (state.target) {
              AppriseNotificationSettingsMutationTarget.GlobalSettings -> settingsErrorMessage
              AppriseNotificationSettingsMutationTarget.NotificationRule -> ruleSaveErrorMessage
              AppriseNotificationSettingsMutationTarget.NotificationRuleEnable ->
                ruleSaveErrorMessage
              AppriseNotificationSettingsMutationTarget.NotificationRuleDelete ->
                ruleDeleteErrorMessage

              AppriseNotificationSettingsMutationTarget.NotificationRuleTest ->
                when (state.reason) {
                  AppriseNotificationSettingsFailureReason.AppriseNotConfigured ->
                    ruleTestNotConfiguredMessage

                  AppriseNotificationSettingsFailureReason.DeliveryFailed ->
                    ruleTestDeliveryFailedMessage

                  null -> ruleTestErrorMessage
                }
            }
        )

      else -> Unit
    }
  }
}

@Composable
private fun apiUrlSupportingText(validation: AppriseGlobalSettingsValidation): String? =
  when (validation.apiUrlError) {
    AppriseGlobalSettingsFieldError.Required -> stringResource(R.string.apprise_api_url_required)
    AppriseGlobalSettingsFieldError.InvalidUrl ->
      stringResource(R.string.enter_a_valid_absolute_url)

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
          apiState =
            AppriseNotificationSettingsApiState.Loading(
              AppriseNotificationSettingsMutationTarget.GlobalSettings
            ),
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
          state = GenericState.Failure("Unable to load Apprise notification settings.")
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AppriseNotificationSettingsAdminOnlyPreview() {
  PreviewWrapper(dynamicColor = false) {
    AppriseNotificationSettingsContent(
      uiState = AppriseNotificationSettingsUiState(state = GenericState.Success)
    )
  }
}
