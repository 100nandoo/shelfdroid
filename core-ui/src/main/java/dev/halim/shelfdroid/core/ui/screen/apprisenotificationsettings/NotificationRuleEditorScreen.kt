package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMutationTarget
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationEventUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.validateNotificationRule
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.withEvent
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ChipInput
import dev.halim.shelfdroid.core.ui.navigation.EditAppriseNotificationRule
import kotlinx.coroutines.launch

@Composable
fun NotificationRuleEditorScreen(
  navKey: EditAppriseNotificationRule,
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
  onSaveSuccess: () -> Unit,
  viewModel: NotificationRuleEditorViewModel =
    hiltViewModel<NotificationRuleEditorViewModel, NotificationRuleEditorViewModel.Factory> { factory ->
      factory.create(navKey)
    },
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val form by viewModel.form.collectAsStateWithLifecycle()

  HandleNotificationRuleEditorSnackbar(
    apiState = uiState.apiState,
    snackbarHostState = snackbarHostState,
    onSaveSuccess = onSaveSuccess,
  )
  NotificationRuleEditorContent(
    uiState = uiState,
    form = form,
    onFormChange = viewModel::updateForm,
    onSave = viewModel::save,
    onCancel = navigateBack,
  )
}

@Composable
private fun NotificationRuleEditorContent(
  uiState: AppriseNotificationSettingsUiState,
  form: NotificationRuleForm,
  onFormChange: ((NotificationRuleForm) -> NotificationRuleForm) -> Unit,
  onSave: () -> Unit,
  onCancel: () -> Unit,
) {
  val isSaving =
    (uiState.apiState as? AppriseNotificationSettingsApiState.Loading)?.target ==
      AppriseNotificationSettingsMutationTarget.NotificationRule

  Column(modifier = Modifier.fillMaxSize()) {
    if (uiState.state is GenericState.Loading || isSaving) {
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

    val validation = validateNotificationRule(form)
    val events = uiState.notificationEvents
    val event = events.firstOrNull { it.name == form.eventName }
    var eventMenuExpanded by remember { mutableStateOf(false) }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp),
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      TextTitleMedium(
        text =
          stringResource(
            if (form.id == null) R.string.create_notification_rule
            else R.string.edit_notification_rule,
          ),
      )
      Spacer(modifier = Modifier.height(16.dp))
      NotificationRuleEditorFields(
        form = form,
        event = event,
        events = events,
        eventMenuExpanded = eventMenuExpanded,
        isSaving = isSaving,
        hasBlankDestinationUrl = validation.hasBlankDestinationUrl,
        onEventMenuChange = { eventMenuExpanded = it },
        onFormChange = onFormChange,
      )
      Spacer(modifier = Modifier.height(24.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        Button(enabled = !isSaving, onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        Spacer(modifier = Modifier.width(8.dp))
        Button(enabled = validation.isValid && !isSaving, onClick = onSave) {
          Text(stringResource(R.string.save))
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun NotificationRuleEditorFields(
  form: NotificationRuleForm,
  event: NotificationEventUi?,
  events: List<NotificationEventUi>,
  eventMenuExpanded: Boolean,
  isSaving: Boolean,
  hasBlankDestinationUrl: Boolean,
  onEventMenuChange: (Boolean) -> Unit,
  onFormChange: ((NotificationRuleForm) -> NotificationRuleForm) -> Unit,
) {
  TextLabelSmall(
    text = stringResource(R.string.event_name),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Button(enabled = events.isNotEmpty() && !isSaving, onClick = { onEventMenuChange(true) }) {
    Text(event?.name ?: form.eventName)
  }
  DropdownMenu(
    expanded = eventMenuExpanded,
    onDismissRequest = { onEventMenuChange(false) },
  ) {
    events.forEach { option ->
      DropdownMenuItem(
        text = { Text(option.name) },
        onClick = {
          onFormChange { it.withEvent(option) }
          onEventMenuChange(false)
        },
      )
    }
  }

  event?.description?.takeIf(String::isNotBlank)?.let {
    Spacer(modifier = Modifier.height(12.dp))
    TextBodyMedium(text = it)
  }

  Spacer(modifier = Modifier.height(12.dp))
  ChipInput(
    label = stringResource(R.string.destination_urls),
    values = form.urls,
    onAdd = { value -> onFormChange { it.copy(urls = it.urls + value) } },
    onRemove = { value -> onFormChange { it.copy(urls = it.urls - value) } },
    enabled = !isSaving,
  )
  if (hasBlankDestinationUrl) {
    TextLabelSmall(
      text = stringResource(R.string.apprise_destination_url_required),
      color = MaterialTheme.colorScheme.error,
    )
  }

  Spacer(modifier = Modifier.height(12.dp))
  OutlinedTextField(
    value = form.titleTemplate,
    onValueChange = { value -> onFormChange { it.copy(titleTemplate = value) } },
    label = { Text(stringResource(R.string.title_template)) },
    modifier = Modifier.fillMaxWidth(),
    enabled = !isSaving,
  )

  Spacer(modifier = Modifier.height(12.dp))
  OutlinedTextField(
    value = form.bodyTemplate,
    onValueChange = { value -> onFormChange { it.copy(bodyTemplate = value) } },
    label = { Text(stringResource(R.string.body_template)) },
    modifier = Modifier.fillMaxWidth(),
    enabled = !isSaving,
  )

  Spacer(modifier = Modifier.height(12.dp))
  MySwitch(
    title = stringResource(R.string.enabled),
    checked = form.enabled,
    contentDescription = stringResource(R.string.enabled),
    enabled = !isSaving,
    onCheckedChange = { value -> onFormChange { it.copy(enabled = value) } },
  )

  event?.variables?.takeIf { it.isNotEmpty() }?.let { variables ->
    Spacer(modifier = Modifier.height(12.dp))
    TextBodyMedium(
      text = stringResource(R.string.notification_rule_variables, variables.joinToString()),
    )
  }
}

@Composable
private fun HandleNotificationRuleEditorSnackbar(
  apiState: AppriseNotificationSettingsApiState,
  snackbarHostState: SnackbarHostState,
  onSaveSuccess: () -> Unit,
) {
  val errorMessage = stringResource(R.string.notification_rule_save_failed)

  LaunchedEffect(apiState) {
    when (apiState) {
      is AppriseNotificationSettingsApiState.Success -> {
        if (apiState.target == AppriseNotificationSettingsMutationTarget.NotificationRule) {
          onSaveSuccess()
        }
      }

      is AppriseNotificationSettingsApiState.Failure -> {
        if (apiState.target == AppriseNotificationSettingsMutationTarget.NotificationRule) {
          launch { snackbarHostState.showErrorSnackbar(apiState.message ?: errorMessage) }
        }
      }

      else -> Unit
    }
  }
}

@ShelfDroidPreview
@Composable
private fun NotificationRuleEditorContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    NotificationRuleEditorContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Success,
          notificationEvents =
            listOf(
              NotificationEventUi(
                name = "onBackupFailed",
                description = "Sent when an Audiobookshelf server backup fails.",
                variables = listOf("{{backupName}}", "{{error}}"),
                defaultTitleTemplate = "Backup failed",
                defaultBodyTemplate = "{{backupName}} failed: {{error}}",
              ),
            ),
        ),
      form =
        NotificationRuleForm(
          eventName = "onBackupFailed",
          urls = listOf("discord://alerts"),
          titleTemplate = "Backup failed",
          bodyTemplate = "{{backupName}} failed: {{error}}",
        ),
      onFormChange = { _ -> },
      onSave = {},
      onCancel = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun NotificationRuleEditorLoadingPreview() {
  PreviewWrapper(dynamicColor = false) {
    NotificationRuleEditorContent(
      uiState = AppriseNotificationSettingsUiState(state = GenericState.Loading),
      form = NotificationRuleForm(),
      onFormChange = { _ -> },
      onSave = {},
      onCancel = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun NotificationRuleEditorErrorPreview() {
  PreviewWrapper(dynamicColor = false) {
    NotificationRuleEditorContent(
      uiState =
        AppriseNotificationSettingsUiState(
          state = GenericState.Failure("Unable to load notification events."),
        ),
      form = NotificationRuleForm(),
      onFormChange = { _ -> },
      onSave = {},
      onCancel = {},
    )
  }
}
