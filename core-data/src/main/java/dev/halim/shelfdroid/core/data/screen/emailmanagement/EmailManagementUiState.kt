package dev.halim.shelfdroid.core.data.screen.emailmanagement

import dev.halim.core.network.response.emailmanagement.EreaderDevice
import dev.halim.shelfdroid.core.data.GenericState

data class EmailManagementUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: EmailManagementApiState = EmailManagementApiState.Idle,
  val savedSettings: EmailSettingsForm = EmailSettingsForm(),
  val draftSettings: EmailSettingsForm = EmailSettingsForm(),
  val devices: List<EreaderDeviceItem> = emptyList(),
  val users: List<UserOption> = emptyList(),
  val hasLoadedUsers: Boolean = false,
  val editorState: DeviceEditorState = DeviceEditorState(),
  val pendingDeleteDeviceName: String? = null,
) {
  val hasChanges: Boolean
    get() = savedSettings != draftSettings

  val canSendTest: Boolean
    get() = !hasChanges && draftSettings.host.isNotBlank()

  val isSavingSettings: Boolean
    get() =
      apiState is EmailManagementApiState.Loading &&
        apiState.operation == EmailManagementOperation.SaveSettings

  val isSendingTest: Boolean
    get() =
      apiState is EmailManagementApiState.Loading &&
        apiState.operation == EmailManagementOperation.SendTest

  val isLoadingUsers: Boolean
    get() =
      apiState is EmailManagementApiState.Loading &&
        apiState.operation == EmailManagementOperation.LoadUsers

  val isUpdatingDevices: Boolean
    get() =
      apiState is EmailManagementApiState.Loading &&
        apiState.operation == EmailManagementOperation.UpdateDevices
}

data class EmailSettingsForm(
  val host: String = "",
  val port: String = "465",
  val secure: Boolean = true,
  val rejectUnauthorized: Boolean = true,
  val user: String = "",
  val pass: String = "",
  val fromAddress: String = "",
  val testAddress: String = "",
)

data class EreaderDeviceItem(
  val name: String = "",
  val email: String = "",
  val availabilityOption: DeviceAvailabilityOption = DeviceAvailabilityOption.AdminOrUp,
  val users: List<String> = emptyList(),
)

data class UserOption(val id: String, val username: String)

data class DeviceEditorState(
  val isVisible: Boolean = false,
  val originalName: String? = null,
  val name: String = "",
  val email: String = "",
  val availabilityOption: DeviceAvailabilityOption = DeviceAvailabilityOption.AdminOrUp,
  val selectedUserIds: List<String> = emptyList(),
  val fieldError: DeviceEditorFieldError = DeviceEditorFieldError.None,
)

data class DeviceEditorFieldError(
  val nameEmpty: Boolean = false,
  val emailEmpty: Boolean = false,
  val duplicateName: Boolean = false,
  val specificUsersEmpty: Boolean = false,
) {
  val hasError: Boolean
    get() = nameEmpty || emailEmpty || duplicateName || specificUsersEmpty

  companion object {
    val None = DeviceEditorFieldError()
  }
}

enum class DeviceAvailabilityOption(val wireValue: String) {
  AdminOrUp("adminOrUp"),
  UserOrUp("userOrUp"),
  GuestOrUp("guestOrUp"),
  SpecificUsers("specificUsers");

  companion object {
    fun fromWireValue(value: String?): DeviceAvailabilityOption =
      entries.firstOrNull { it.wireValue == value } ?: AdminOrUp
  }
}

enum class DeviceMutation {
  Create,
  Update,
  Delete,
}

enum class EmailManagementOperation {
  SaveSettings,
  SendTest,
  LoadUsers,
  UpdateDevices,
}

sealed interface EmailManagementApiState {
  data object Idle : EmailManagementApiState

  data class Loading(val operation: EmailManagementOperation) : EmailManagementApiState

  data class Success(
    val operation: EmailManagementOperation,
    val deviceMutation: DeviceMutation? = null,
  ) : EmailManagementApiState

  data class Failure(val operation: EmailManagementOperation, val message: String?) :
    EmailManagementApiState
}

fun validateDeviceEditor(
  editorState: DeviceEditorState,
  existingDevices: List<EreaderDeviceItem>,
): DeviceEditorFieldError {
  val trimmedName = editorState.name.trim()
  val duplicateName = existingDevices.any { device ->
    device.name == trimmedName && device.name != editorState.originalName
  }
  return DeviceEditorFieldError(
    nameEmpty = trimmedName.isEmpty(),
    emailEmpty = editorState.email.trim().isEmpty(),
    duplicateName = duplicateName,
    specificUsersEmpty =
      editorState.availabilityOption == DeviceAvailabilityOption.SpecificUsers &&
        editorState.selectedUserIds.isEmpty(),
  )
}

fun buildUpdatedDeviceList(
  existingDevices: List<EreaderDeviceItem>,
  originalName: String?,
  updatedDevice: EreaderDeviceItem,
): List<EreaderDeviceItem> {
  val filtered =
    if (originalName == null) {
      existingDevices
    } else {
      existingDevices.filterNot { it.name == originalName }
    }
  return filtered + updatedDevice
}

fun buildDeletedDeviceList(
  existingDevices: List<EreaderDeviceItem>,
  deviceName: String,
): List<EreaderDeviceItem> = existingDevices.filterNot { it.name == deviceName }

fun DeviceEditorState.toDeviceItem(): EreaderDeviceItem =
  EreaderDeviceItem(
    name = name.trim(),
    email = email.trim(),
    availabilityOption = availabilityOption,
    users =
      if (availabilityOption == DeviceAvailabilityOption.SpecificUsers) selectedUserIds
      else emptyList(),
  )

fun EreaderDeviceItem.toNetwork(): EreaderDevice =
  EreaderDevice(
    name = name,
    email = email,
    availabilityOption = availabilityOption.wireValue,
    users = users,
  )
