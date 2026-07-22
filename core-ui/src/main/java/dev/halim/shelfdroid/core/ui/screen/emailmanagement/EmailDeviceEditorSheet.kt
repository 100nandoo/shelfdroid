@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.emailmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions as ComposeKeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceAvailabilityOption
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceEditorFieldError
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceEditorState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementApiState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementOperation
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementUiState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailSettingsForm
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EreaderDeviceItem
import dev.halim.shelfdroid.core.data.screen.emailmanagement.UserOption
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DropdownOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState

@Composable
internal fun EmailDeviceEditorSheet(
  uiState: EmailManagementUiState,
  sheetState: SheetState,
  onEvent: (EmailManagementEvent) -> Unit = {},
) {
  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = { onEvent(EmailManagementEvent.DismissDeviceEditor) },
  ) {
    EmailDeviceEditorContent(uiState = uiState, onEvent = onEvent)
  }
}

@Composable
internal fun EmailDeviceEditorContent(
  uiState: EmailManagementUiState,
  onEvent: (EmailManagementEvent) -> Unit = {},
) {
  val editor = uiState.editorState
  val availabilityOptions = remember {
    listOf(
      DeviceAvailabilityOption.AdminOrUp,
      DeviceAvailabilityOption.UserOrUp,
      DeviceAvailabilityOption.GuestOrUp,
      DeviceAvailabilityOption.SpecificUsers,
    )
  }
  val availabilityLabels = availabilityOptions.associateWith { option -> availabilityLabel(option) }
  val selectedUsernames =
    remember(uiState.users, editor.selectedUserIds) {
      editor.selectedUserIds.mapNotNull { id ->
        uiState.users.find { it.id == id }?.username
      }
    }
  val userOptions = remember(uiState.users) { uiState.users.map { it.username } }

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
    TextTitleMedium(
      text =
        if (editor.originalName == null) stringResource(R.string.add_device)
        else stringResource(R.string.edit_device)
    )
    Spacer(modifier = Modifier.height(16.dp))
    MyOutlinedTextField(
      value = editor.name,
      onValueChange = { value ->
        onEvent(EmailManagementEvent.UpdateDeviceEditor { it.copy(name = value) })
      },
      label = stringResource(R.string.name),
      isError = editor.fieldError.nameEmpty || editor.fieldError.duplicateName,
      supportingText =
        when {
          editor.fieldError.duplicateName -> stringResource(R.string.device_name_already_exists)
          editor.fieldError.nameEmpty -> stringResource(R.string.device_name_required)
          else -> null
        },
      keyboardOptions =
        ComposeKeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
    )
    Spacer(modifier = Modifier.height(12.dp))
    MyOutlinedTextField(
      value = editor.email,
      onValueChange = { value ->
        onEvent(EmailManagementEvent.UpdateDeviceEditor { it.copy(email = value) })
      },
      label = stringResource(R.string.email_address),
      isError = editor.fieldError.emailEmpty,
      supportingText =
        if (editor.fieldError.emailEmpty) stringResource(R.string.device_email_required) else null,
      keyboardOptions =
        ComposeKeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
    Spacer(modifier = Modifier.height(12.dp))
    AvailabilityDropdownField(
      selectedOption = editor.availabilityOption,
      options = availabilityOptions,
      optionLabels = availabilityLabels,
      onOptionSelected = { selectedOption ->
        onEvent(
          EmailManagementEvent.UpdateDeviceEditor {
            it.copy(
              availabilityOption = selectedOption,
              selectedUserIds =
                if (selectedOption == DeviceAvailabilityOption.SpecificUsers) {
                  it.selectedUserIds
                } else {
                  emptyList()
                },
            )
          }
        )
        if (selectedOption == DeviceAvailabilityOption.SpecificUsers) {
          onEvent(EmailManagementEvent.LoadUsersIfNeeded)
        }
      },
    )
    if (editor.availabilityOption == DeviceAvailabilityOption.SpecificUsers) {
      Spacer(modifier = Modifier.height(12.dp))
      DropdownOutlinedTextField(
        selectedOptions = selectedUsernames.sorted(),
        onOptionToggled = { username ->
          val userId =
            uiState.users.find { it.username == username }?.id ?: return@DropdownOutlinedTextField
          onEvent(
            EmailManagementEvent.UpdateDeviceEditor {
              val newSelection =
                if (userId in it.selectedUserIds) it.selectedUserIds - userId
                else it.selectedUserIds + userId
              it.copy(selectedUserIds = newSelection)
            }
          )
        },
        onOptionRemoved = { username ->
          val userId =
            uiState.users.find { it.username == username }?.id ?: return@DropdownOutlinedTextField
          onEvent(
            EmailManagementEvent.UpdateDeviceEditor {
              it.copy(selectedUserIds = it.selectedUserIds - userId)
            }
          )
        },
        label = stringResource(R.string.users),
        options = userOptions,
        placeholder =
          if (uiState.isLoadingUsers) {
            stringResource(R.string.loading_users)
          } else {
            stringResource(R.string.select_users)
          },
        supportingText =
          when {
            editor.fieldError.specificUsersEmpty ->
              stringResource(R.string.select_at_least_one_user)
            userOptions.isEmpty() && uiState.hasLoadedUsers ->
              stringResource(R.string.no_users_available)
            else -> null
          },
        isError = editor.fieldError.specificUsersEmpty,
      )
    }
    if (uiState.isLoadingUsers) {
      Spacer(modifier = Modifier.height(12.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      TextButton(onClick = { onEvent(EmailManagementEvent.DismissDeviceEditor) }) {
        Text(stringResource(R.string.cancel))
      }
      Button(
        enabled = !uiState.isUpdatingDevices,
        onClick = { onEvent(EmailManagementEvent.SubmitDeviceEditor) },
      ) {
        Text(stringResource(R.string.save))
      }
    }
  }
}

@Composable
private fun AvailabilityDropdownField(
  selectedOption: DeviceAvailabilityOption,
  options: List<DeviceAvailabilityOption>,
  optionLabels: Map<DeviceAvailabilityOption, String>,
  onOptionSelected: (DeviceAvailabilityOption) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = Modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = optionLabels[selectedOption].orEmpty(),
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(R.string.device_available_to)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier =
        Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(optionLabels[option].orEmpty()) },
          onClick = {
            expanded = false
            onOptionSelected(option)
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

@Composable
internal fun availabilityLabel(option: DeviceAvailabilityOption): String =
  when (option) {
    DeviceAvailabilityOption.AdminOrUp -> stringResource(R.string.admins_only)
    DeviceAvailabilityOption.UserOrUp -> stringResource(R.string.users_excluding_guests)
    DeviceAvailabilityOption.GuestOrUp -> stringResource(R.string.users_including_guests)
    DeviceAvailabilityOption.SpecificUsers -> stringResource(R.string.select_users)
  }

@ShelfDroidPreview
@Composable
private fun EmailDeviceEditorContentCreatePreview() {
  PreviewWrapper(dynamicColor = false) {
    EmailDeviceEditorContent(uiState = previewEmailManagementUiState())
  }
}

@ShelfDroidPreview
@Composable
private fun EmailDeviceEditorContentSpecificUsersPreview() {
  PreviewWrapper(dynamicColor = false) {
    EmailDeviceEditorContent(
      uiState =
        previewEmailManagementUiState(
          apiState = EmailManagementApiState.Loading(EmailManagementOperation.LoadUsers),
          editorState =
            DeviceEditorState(
              isVisible = true,
              originalName = "Kindle",
              name = "Kindle",
              email = "kindle@example.com",
              availabilityOption = DeviceAvailabilityOption.SpecificUsers,
              fieldError = DeviceEditorFieldError(specificUsersEmpty = true),
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun EmailDeviceEditorSheetPreview() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val state = sheetState(density)

    EmailDeviceEditorSheet(
      uiState =
        previewEmailManagementUiState(
          editorState =
            DeviceEditorState(
              isVisible = true,
              originalName = "Kindle",
              name = "Kindle Paperwhite",
              email = "paperwhite@example.com",
              availabilityOption = DeviceAvailabilityOption.SpecificUsers,
              selectedUserIds = listOf("user-1"),
            )
        ),
      sheetState = state,
      onEvent = {},
    )
    LaunchedEffect(Unit) { state.show() }
  }
}

internal fun previewEmailManagementUiState(
  apiState: EmailManagementApiState = EmailManagementApiState.Idle,
  editorState: DeviceEditorState = DeviceEditorState(isVisible = true),
): EmailManagementUiState =
  EmailManagementUiState(
    state = GenericState.Success,
    apiState = apiState,
    savedSettings =
      EmailSettingsForm(
        host = "smtp.example.com",
        user = "cross",
        fromAddress = "library@example.com",
        testAddress = "cross@example.com",
      ),
    draftSettings =
      EmailSettingsForm(
        host = "smtp.example.com",
        user = "cross",
        fromAddress = "library@example.com",
        testAddress = "cross@example.com",
      ),
    devices =
      listOf(
        EreaderDeviceItem(
          name = "Kindle",
          email = "kindle@example.com",
          availabilityOption = DeviceAvailabilityOption.SpecificUsers,
          users = listOf("user-1"),
        ),
        EreaderDeviceItem(
          name = "Kobo",
          email = "kobo@example.com",
          availabilityOption = DeviceAvailabilityOption.AdminOrUp,
        ),
      ),
    users = previewUsers(),
    hasLoadedUsers = true,
    editorState = editorState,
  )

internal fun previewUsers(): List<UserOption> =
  listOf(
    UserOption(id = "user-1", username = "cross"),
    UserOption(id = "user-2", username = "guest-reader"),
    UserOption(id = "user-3", username = "ops"),
  )
