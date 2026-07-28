package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings.notificationruleeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.navigation.EditAppriseNotificationRule
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ChipInput
import kotlinx.coroutines.launch

private enum class NotificationRuleTemplateField {
  Title,
  Body,
}

@Composable
fun NotificationRuleEditorScreen(
  navKey: EditAppriseNotificationRule,
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
  onSaveSuccess: () -> Unit,
  viewModel: NotificationRuleEditorViewModel =
    hiltViewModel<NotificationRuleEditorViewModel, NotificationRuleEditorViewModel.Factory> {
      factory ->
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
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      TextTitleMedium(
        text =
          stringResource(
            if (form.id == null) R.string.create_notification_rule
            else R.string.edit_notification_rule
          )
      )
      Spacer(modifier = Modifier.height(16.dp))
      NotificationRuleEditorFields(
        form = form,
        event = event,
        events = events,
        isSaving = isSaving,
        hasBlankDestinationUrl = validation.hasBlankDestinationUrl,
        onFormChange = onFormChange,
      )
      Spacer(modifier = Modifier.height(24.dp))
      if (form.id == null) {
        Button(
          enabled = validation.isValid && !isSaving,
          onClick = onSave,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(R.string.submit))
        }
      } else {
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
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationRuleEditorFields(
  form: NotificationRuleForm,
  event: NotificationEventUi?,
  events: List<NotificationEventUi>,
  isSaving: Boolean,
  hasBlankDestinationUrl: Boolean,
  onFormChange: ((NotificationRuleForm) -> NotificationRuleForm) -> Unit,
) {
  val ruleId = form.id
  val eventModifier =
    if (ruleId == null) {
      Modifier.fillMaxWidth()
    } else {
      Modifier.fillMaxWidth()
        .mySharedElement(Animations.Companion.NotificationRule.eventNameKey(ruleId))
    }
  val destinationModifier =
    if (ruleId == null) {
      Modifier
    } else {
      Modifier.mySharedElement(Animations.Companion.NotificationRule.destinationKey(ruleId))
    }
  var focusedTemplateField by remember { mutableStateOf<NotificationRuleTemplateField?>(null) }
  var titleTemplateFieldValue by remember {
    mutableStateOf(
      TextFieldValue(
        text = form.titleTemplate,
        selection = TextRange(form.titleTemplate.length),
      )
    )
  }
  var bodyTemplateFieldValue by remember {
    mutableStateOf(
      TextFieldValue(
        text = form.bodyTemplate,
        selection = TextRange(form.bodyTemplate.length),
      )
    )
  }

  LaunchedEffect(form.titleTemplate) {
    if (form.titleTemplate != titleTemplateFieldValue.text) {
      titleTemplateFieldValue =
        TextFieldValue(
          text = form.titleTemplate,
          selection = TextRange(form.titleTemplate.length),
        )
    }
  }
  LaunchedEffect(form.bodyTemplate) {
    if (form.bodyTemplate != bodyTemplateFieldValue.text) {
      bodyTemplateFieldValue =
        TextFieldValue(
          text = form.bodyTemplate,
          selection = TextRange(form.bodyTemplate.length),
        )
    }
  }

  fun updateTitleTemplate(value: TextFieldValue) {
    titleTemplateFieldValue = value
    onFormChange { it.copy(titleTemplate = value.text) }
  }

  fun updateBodyTemplate(value: TextFieldValue) {
    bodyTemplateFieldValue = value
    onFormChange { it.copy(bodyTemplate = value.text) }
  }

  fun insertVariableIntoFocusedTemplate(variable: String) {
    when (focusedTemplateField) {
      NotificationRuleTemplateField.Title ->
        updateTitleTemplate(insertNotificationRuleVariable(titleTemplateFieldValue, variable))

      NotificationRuleTemplateField.Body ->
        updateBodyTemplate(insertNotificationRuleVariable(bodyTemplateFieldValue, variable))

      null -> Unit
    }
  }

  ChipDropdownMenu(
    modifier = eventModifier,
    options = events.map(NotificationEventUi::name),
    label = stringResource(R.string.event_name),
    labelPosition = LabelPosition.Top,
    initialValue = event?.name ?: form.eventName,
    enabled = !isSaving,
    onClick = { name ->
      events
        .firstOrNull { it.name == name }
        ?.let { option ->
          onFormChange { currentForm -> currentForm.withEvent(option) }
        }
    },
  )

  event?.description?.takeIf(String::isNotBlank)?.let {
    TextLabelMedium(text = it, color = OutlinedTextFieldDefaults.colors().unfocusedLabelColor)
  }

  Spacer(modifier = Modifier.height(12.dp))
  ChipInput(
    modifier = destinationModifier,
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
    value = titleTemplateFieldValue,
    onValueChange = ::updateTitleTemplate,
    label = { Text(stringResource(R.string.title_template)) },
    modifier =
      Modifier.fillMaxWidth().onFocusChanged { state ->
        focusedTemplateField =
          if (state.isFocused) NotificationRuleTemplateField.Title
          else if (focusedTemplateField == NotificationRuleTemplateField.Title) null
          else focusedTemplateField
      },
    enabled = !isSaving,
  )

  Spacer(modifier = Modifier.height(12.dp))
  OutlinedTextField(
    value = bodyTemplateFieldValue,
    onValueChange = ::updateBodyTemplate,
    label = { Text(stringResource(R.string.body_template)) },
    modifier =
      Modifier.fillMaxWidth().onFocusChanged { state ->
        focusedTemplateField =
          if (state.isFocused) NotificationRuleTemplateField.Body
          else if (focusedTemplateField == NotificationRuleTemplateField.Body) null
          else focusedTemplateField
      },
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

  event
    ?.variables
    ?.takeIf { it.isNotEmpty() }
    ?.let { variables ->
      Spacer(modifier = Modifier.height(12.dp))
      TextBodyMedium(text = stringResource(R.string.notification_rule_variables_label))
      Spacer(modifier = Modifier.height(4.dp))
      TextLabelSmall(
        text = stringResource(R.string.notification_rule_variables_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        variables.forEach { variable ->
          val variableName = notificationRuleVariableName(variable)
          val formattedVariable = formatNotificationRuleTemplateVariable(variable)
          InputChip(
            selected = false,
            onClick = { insertVariableIntoFocusedTemplate(formattedVariable) },
            enabled = !isSaving,
            label = { Text(variableName) },
            modifier = Modifier.focusProperties { canFocus = false },
          )
        }
      }
    }
}

internal fun notificationRuleVariableName(variable: String): String {
  val normalizedVariable = variable.trim().removePrefix("{{").removeSuffix("}}").trim()
  return if (normalizedVariable.isBlank()) variable.trim() else normalizedVariable
}

internal fun formatNotificationRuleTemplateVariable(variable: String): String {
  val normalizedVariable = notificationRuleVariableName(variable)
  return if (normalizedVariable.isBlank()) variable.trim() else "{{ $normalizedVariable }}"
}

internal fun insertNotificationRuleVariable(
  currentValue: TextFieldValue,
  variable: String,
): TextFieldValue {
  val selectionStart = currentValue.selection.min.coerceIn(0, currentValue.text.length)
  val selectionEnd = currentValue.selection.max.coerceIn(0, currentValue.text.length)
  val updatedText = currentValue.text.replaceRange(selectionStart, selectionEnd, variable)
  val updatedSelection = TextRange(selectionStart + variable.length)

  return currentValue.copy(text = updatedText, selection = updatedSelection)
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
              )
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
          state = GenericState.Failure("Unable to load notification events.")
        ),
      form = NotificationRuleForm(),
      onFormChange = { _ -> },
      onSave = {},
      onCancel = {},
    )
  }
}
