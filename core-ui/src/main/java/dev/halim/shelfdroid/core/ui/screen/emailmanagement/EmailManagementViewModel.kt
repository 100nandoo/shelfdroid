package dev.halim.shelfdroid.core.ui.screen.emailmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceEditorFieldError
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceEditorState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.DeviceMutation
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementApiState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementOperation
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementRepository
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailManagementUiState
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EmailSettingsForm
import dev.halim.shelfdroid.core.data.screen.emailmanagement.buildDeletedDeviceList
import dev.halim.shelfdroid.core.data.screen.emailmanagement.buildUpdatedDeviceList
import dev.halim.shelfdroid.core.data.screen.emailmanagement.toDeviceItem
import dev.halim.shelfdroid.core.data.screen.emailmanagement.validateDeviceEditor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EmailManagementViewModel
@Inject
constructor(private val repository: EmailManagementRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(EmailManagementUiState())
  val uiState: StateFlow<EmailManagementUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EmailManagementUiState())

  fun onEvent(event: EmailManagementEvent) {
    when (event) {
      is EmailManagementEvent.UpdateDraftSettings -> {
        _uiState.update { it.copy(draftSettings = event.transform(it.draftSettings)) }
      }
      EmailManagementEvent.ResetDraftSettings -> {
        _uiState.update { it.copy(draftSettings = it.savedSettings) }
      }
      EmailManagementEvent.SaveSettings -> saveSettings()
      EmailManagementEvent.SendTestEmail -> sendTestEmail()
      EmailManagementEvent.OpenCreateDeviceEditor -> {
        _uiState.update { it.copy(editorState = DeviceEditorState(isVisible = true)) }
      }
      is EmailManagementEvent.OpenEditDeviceEditor -> openEditDeviceEditor(event.deviceName)
      EmailManagementEvent.DismissDeviceEditor -> {
        _uiState.update { it.copy(editorState = DeviceEditorState()) }
      }
      is EmailManagementEvent.UpdateDeviceEditor -> {
        _uiState.update {
          it.copy(
            editorState =
              event.transform(it.editorState).copy(fieldError = DeviceEditorFieldError.None)
          )
        }
      }
      EmailManagementEvent.LoadUsersIfNeeded -> loadUsersIfNeeded()
      EmailManagementEvent.SubmitDeviceEditor -> submitDeviceEditor()
      is EmailManagementEvent.RequestDeleteDevice -> {
        _uiState.update { it.copy(pendingDeleteDeviceName = event.deviceName) }
      }
      EmailManagementEvent.DismissDeleteDialog -> {
        _uiState.update { it.copy(pendingDeleteDeviceName = null) }
      }
      EmailManagementEvent.ConfirmDeleteDevice -> deleteDevice()
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.load() } }
  }

  private fun saveSettings() {
    viewModelScope.launch {
      _uiState.update {
        it.copy(apiState = EmailManagementApiState.Loading(EmailManagementOperation.SaveSettings))
      }
      _uiState.update { repository.saveSettings(it) }
    }
  }

  private fun sendTestEmail() {
    if (!_uiState.value.canSendTest) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(apiState = EmailManagementApiState.Loading(EmailManagementOperation.SendTest))
      }
      _uiState.update { repository.sendTestEmail(it) }
    }
  }

  private fun loadUsersIfNeeded() {
    val currentState = _uiState.value
    if (currentState.hasLoadedUsers || currentState.isLoadingUsers) return

    viewModelScope.launch {
      _uiState.update {
        it.copy(apiState = EmailManagementApiState.Loading(EmailManagementOperation.LoadUsers))
      }
      _uiState.update { repository.loadUsers(it) }
    }
  }

  private fun openEditDeviceEditor(deviceName: String) {
    val device = _uiState.value.devices.find { it.name == deviceName } ?: return
    _uiState.update {
      it.copy(
        editorState =
          DeviceEditorState(
            isVisible = true,
            originalName = device.name,
            name = device.name,
            email = device.email,
            availabilityOption = device.availabilityOption,
            selectedUserIds = device.users,
          )
      )
    }
    if (device.users.isNotEmpty()) {
      loadUsersIfNeeded()
    }
  }

  private fun submitDeviceEditor() {
    val currentState = _uiState.value
    val validation = validateDeviceEditor(currentState.editorState, currentState.devices)
    if (validation.hasError) {
      _uiState.update { it.copy(editorState = it.editorState.copy(fieldError = validation)) }
      return
    }

    val updatedDevice = currentState.editorState.toDeviceItem()
    val updatedDevices =
      buildUpdatedDeviceList(
        existingDevices = currentState.devices,
        originalName = currentState.editorState.originalName,
        updatedDevice = updatedDevice,
      )
    val mutation =
      if (currentState.editorState.originalName == null) DeviceMutation.Create
      else DeviceMutation.Update
    updateDevices(updatedDevices, mutation)
  }

  private fun deleteDevice() {
    val deviceName = _uiState.value.pendingDeleteDeviceName ?: return
    updateDevices(
      devices = buildDeletedDeviceList(_uiState.value.devices, deviceName),
      mutation = DeviceMutation.Delete,
    )
  }

  private fun updateDevices(
    devices: List<dev.halim.shelfdroid.core.data.screen.emailmanagement.EreaderDeviceItem>,
    mutation: DeviceMutation,
  ) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(apiState = EmailManagementApiState.Loading(EmailManagementOperation.UpdateDevices))
      }
      _uiState.update { repository.updateDevices(it, devices, mutation) }
    }
  }
}

sealed interface EmailManagementEvent {
  data class UpdateDraftSettings(val transform: (EmailSettingsForm) -> EmailSettingsForm) :
    EmailManagementEvent

  data object ResetDraftSettings : EmailManagementEvent

  data object SaveSettings : EmailManagementEvent

  data object SendTestEmail : EmailManagementEvent

  data object OpenCreateDeviceEditor : EmailManagementEvent

  data class OpenEditDeviceEditor(val deviceName: String) : EmailManagementEvent

  data object DismissDeviceEditor : EmailManagementEvent

  data class UpdateDeviceEditor(val transform: (DeviceEditorState) -> DeviceEditorState) :
    EmailManagementEvent

  data object LoadUsersIfNeeded : EmailManagementEvent

  data object SubmitDeviceEditor : EmailManagementEvent

  data class RequestDeleteDevice(val deviceName: String) : EmailManagementEvent

  data object DismissDeleteDialog : EmailManagementEvent

  data object ConfirmDeleteDevice : EmailManagementEvent
}
