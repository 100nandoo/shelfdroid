@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.emailmanagement

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceAvailabilityOption
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceMutation
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementApiState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementOperation
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementUiState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EreaderDeviceItem
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.MyTonalIconButton
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
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
fun EmailManagementScreen(
  viewModel: EmailManagementViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HandleEmailManagementSnackbar(uiState, snackbarHostState)
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  if (uiState.editorState.isVisible) {
    EmailDeviceEditorSheet(
      uiState = uiState,
      sheetState = sheetState,
      onEvent = viewModel::onEvent,
    )
  }

  MyAlertDialog(
    showDialog = uiState.pendingDeleteDeviceName != null,
    title = stringResource(R.string.delete_device),
    text =
      stringResource(
        R.string.delete_device_confirm,
        uiState.pendingDeleteDeviceName.orEmpty(),
      ),
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = { viewModel.onEvent(EmailManagementEvent.ConfirmDeleteDevice) },
    onDismiss = { viewModel.onEvent(EmailManagementEvent.DismissDeleteDialog) },
  )

  EmailManagementContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun EmailManagementContent(
  uiState: EmailManagementUiState = EmailManagementUiState(),
  onEvent: (EmailManagementEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize()) {
    VisibilityDown(
      uiState.state is GenericState.Loading || uiState.apiState is EmailManagementApiState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    val state = uiState.state
    if (state is GenericState.Failure) {
      GenericMessageScreen(state.errorMessage ?: "")
      return
    }

    Column(
      modifier =
        Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      EmailSettingsSection(uiState = uiState, onEvent = onEvent)
      Spacer(modifier = Modifier.height(24.dp))
      EreaderDevicesSection(uiState = uiState, onEvent = onEvent)
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun EmailSettingsSection(
  uiState: EmailManagementUiState,
  onEvent: (EmailManagementEvent) -> Unit,
) {
  val draft = uiState.draftSettings

  TextTitleMedium(text = stringResource(R.string.smtp_settings))
  TextLabelSmall(
    modifier = Modifier.padding(top = 4.dp),
    text = stringResource(R.string.smtp_settings_description),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Spacer(modifier = Modifier.height(12.dp))

  MyOutlinedTextField(
    value = draft.host,
    onValueChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(host = value) })
    },
    label = stringResource(R.string.host),
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
  )
  Spacer(modifier = Modifier.height(12.dp))
  MyOutlinedTextField(
    value = draft.port,
    onValueChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(port = value) })
    },
    label = stringResource(R.string.port),
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
  )
  Spacer(modifier = Modifier.height(8.dp))
  MySwitch(
    title = stringResource(R.string.secure_connection),
    checked = draft.secure,
    contentDescription = stringResource(R.string.secure_connection),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
    onCheckedChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(secure = value) })
    },
  )
  MySwitch(
    title = stringResource(R.string.reject_unauthorized_certificates),
    checked = draft.rejectUnauthorized,
    contentDescription = stringResource(R.string.reject_unauthorized_certificates),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
    onCheckedChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(rejectUnauthorized = value) })
    },
  )
  Spacer(modifier = Modifier.height(4.dp))
  MyOutlinedTextField(
    value = draft.user,
    onValueChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(user = value) })
    },
    label = stringResource(R.string.username),
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
  )
  Spacer(modifier = Modifier.height(12.dp))
  PasswordTextField(
    value = draft.pass,
    onValueChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(pass = value) })
    },
    label = stringResource(R.string.password),
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
  )
  Spacer(modifier = Modifier.height(12.dp))
  MyOutlinedTextField(
    value = draft.fromAddress,
    onValueChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(fromAddress = value) })
    },
    label = stringResource(R.string.from_address),
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
  )
  Spacer(modifier = Modifier.height(12.dp))
  MyOutlinedTextField(
    value = draft.testAddress,
    onValueChange = { value ->
      onEvent(EmailManagementEvent.UpdateDraftSettings { it.copy(testAddress = value) })
    },
    label = stringResource(R.string.test_address),
    keyboardOptions =
      ComposeKeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
    enabled = !uiState.isSavingSettings && !uiState.isSendingTest,
  )
  Spacer(modifier = Modifier.height(12.dp))
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    if (uiState.hasChanges) {
      TextButton(
        enabled = !uiState.isSavingSettings,
        onClick = { onEvent(EmailManagementEvent.ResetDraftSettings) },
      ) {
        Text(stringResource(R.string.reset))
      }
    } else {
      TextButton(
        enabled = uiState.canSendTest && !uiState.isSendingTest && !uiState.isSavingSettings,
        onClick = { onEvent(EmailManagementEvent.SendTestEmail) },
      ) {
        Text(stringResource(R.string.test))
      }
    }
    Button(
      enabled = uiState.hasChanges && !uiState.isSavingSettings && !uiState.isSendingTest,
      onClick = { onEvent(EmailManagementEvent.SaveSettings) },
    ) {
      Text(stringResource(R.string.save))
    }
  }
}

@Composable
private fun EreaderDevicesSection(
  uiState: EmailManagementUiState,
  onEvent: (EmailManagementEvent) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Column(modifier = Modifier.weight(1f)) {
      TextTitleMedium(text = stringResource(R.string.e_reader_devices))
      TextLabelSmall(
        modifier = Modifier.padding(top = 4.dp, end = 16.dp),
        text = stringResource(R.string.e_reader_devices_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    TextButton(
      enabled = !uiState.isUpdatingDevices,
      onClick = { onEvent(EmailManagementEvent.OpenCreateDeviceEditor) },
    ) {
      Text(stringResource(R.string.add_device))
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  if (uiState.devices.isEmpty()) {
    TextLabelSmall(
      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
      text = stringResource(R.string.no_e_reader_devices),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    return
  }

  Column {
    uiState.devices.forEach { device ->
      HorizontalDivider()
      EreaderDeviceRow(
        device = device,
        accessibleBy = accessibleByLabel(device, uiState),
        onEditClick = {
          onEvent(EmailManagementEvent.OpenEditDeviceEditor(device.name))
        },
        onDeleteClick = {
          onEvent(EmailManagementEvent.RequestDeleteDevice(device.name))
        },
      )
    }
    HorizontalDivider()
  }
}

@Composable
private fun EreaderDeviceRow(
  device: EreaderDeviceItem,
  accessibleBy: String,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      TextTitleSmall(text = device.name)
      TextLabelSmall(
        modifier = Modifier.padding(top = 4.dp),
        text = device.email,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TextLabelSmall(
        modifier = Modifier.padding(top = 4.dp),
        text = stringResource(R.string.accessible_by_value, accessibleBy),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      MyTonalIconButton(
        painterResId = R.drawable.edit,
        contentDescriptionResId = R.string.edit_device,
        onClick = onEditClick,
      )
      MyTonalIconButton(
        painterResId = R.drawable.delete,
        contentDescriptionResId = R.string.delete_device,
        onClick = onDeleteClick,
      )
    }
  }
}

@Composable
private fun HandleEmailManagementSnackbar(
  uiState: EmailManagementUiState,
  snackbarHostState: SnackbarHostState,
) {
  val settingsUpdatedMessage = stringResource(R.string.email_settings_updated)
  val settingsUpdateFailedMessage = stringResource(R.string.email_settings_update_failed)
  val testSuccessMessage = stringResource(R.string.test_email_sent)
  val testFailureMessage = stringResource(R.string.test_email_failed)
  val usersFailureMessage = stringResource(R.string.failed_to_load_users)
  val deviceFailureMessage = stringResource(R.string.device_update_failed)
  val deviceCreatedMessage = stringResource(R.string.device_added)
  val deviceUpdatedMessage = stringResource(R.string.device_updated)
  val deviceDeletedMessage = stringResource(R.string.device_deleted)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is EmailManagementApiState.Failure -> {
        val fallbackMessage =
          when (state.operation) {
            EmailManagementOperation.SaveSettings -> settingsUpdateFailedMessage
            EmailManagementOperation.SendTest -> testFailureMessage
            EmailManagementOperation.LoadUsers -> usersFailureMessage
            EmailManagementOperation.UpdateDevices -> deviceFailureMessage
          }
        snackbarHostState.showErrorSnackbar(state.message ?: fallbackMessage)
      }

      is EmailManagementApiState.Success -> {
        when (state.operation) {
          EmailManagementOperation.SaveSettings -> {
            snackbarHostState.showSuccessSnackbar(settingsUpdatedMessage)
          }

          EmailManagementOperation.SendTest -> {
            snackbarHostState.showSuccessSnackbar(testSuccessMessage)
          }

          EmailManagementOperation.UpdateDevices -> {
            val message =
              when (state.deviceMutation) {
                DeviceMutation.Create -> deviceCreatedMessage
                DeviceMutation.Update -> deviceUpdatedMessage
                DeviceMutation.Delete -> deviceDeletedMessage
                null -> deviceUpdatedMessage
              }
            snackbarHostState.showSuccessSnackbar(message)
          }

          EmailManagementOperation.LoadUsers -> Unit
        }
      }

      else -> Unit
    }
  }
}

@Composable
private fun accessibleByLabel(device: EreaderDeviceItem, uiState: EmailManagementUiState): String =
  when (device.availabilityOption) {
    DeviceAvailabilityOption.AdminOrUp -> stringResource(R.string.admins_only)
    DeviceAvailabilityOption.UserOrUp -> stringResource(R.string.users_excluding_guests)
    DeviceAvailabilityOption.GuestOrUp -> stringResource(R.string.users_including_guests)
    DeviceAvailabilityOption.SpecificUsers -> {
      val usernames =
        device.users
          .mapNotNull { id -> uiState.users.find { it.id == id }?.username }
          .ifEmpty {
            device.users
          }
      usernames.joinToString()
    }
  }

@ShelfDroidPreview
@Composable
private fun EmailManagementContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    EmailManagementContent(
      uiState =
        previewEmailManagementUiState(
          editorState = dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceEditorState()
        )
    )
  }
}
