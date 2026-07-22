package dev.halim.shelfdroid.core.data.screen.emailmanagement

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.emailmanagement.UpdateEmailSettingsRequest
import dev.halim.core.network.request.emailmanagement.UpdateEreaderDevicesRequest
import dev.halim.core.network.response.emailmanagement.EmailSettings
import dev.halim.core.network.response.emailmanagement.EreaderDevice
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.UserRepo
import javax.inject.Inject

class EmailManagementRepository
@Inject
constructor(
  private val api: ApiService,
  private val userRepo: UserRepo,
) {

  suspend fun load(): EmailManagementUiState {
    val settingsResult =
      api.emailSettings().getOrElse {
        return EmailManagementUiState(state = GenericState.Failure(it.message))
      }

    val devices = settingsResult.settings.ereaderDevices.map(EreaderDevice::toItem)
    val shouldLoadUsers = devices.any {
      it.availabilityOption == DeviceAvailabilityOption.SpecificUsers
    }
    val usersResult = if (shouldLoadUsers) loadUsersInternal() else Result.success(emptyList())

    val users = usersResult.getOrElse { emptyList() }
    val apiState =
      usersResult.exceptionOrNull()?.message?.let {
        EmailManagementApiState.Failure(EmailManagementOperation.LoadUsers, it)
      } ?: EmailManagementApiState.Idle

    return EmailManagementUiState(
      state = GenericState.Success,
      apiState = apiState,
      savedSettings = settingsResult.settings.toForm(),
      draftSettings = settingsResult.settings.toForm(),
      devices = devices,
      users = users,
      hasLoadedUsers = shouldLoadUsers && usersResult.isSuccess,
    )
  }

  suspend fun saveSettings(uiState: EmailManagementUiState): EmailManagementUiState {
    if (!uiState.hasChanges) return uiState.copy(apiState = EmailManagementApiState.Idle)

    val response =
      api.updateEmailSettings(uiState.draftSettings.toRequest()).getOrElse {
        return uiState.copy(
          apiState =
            EmailManagementApiState.Failure(EmailManagementOperation.SaveSettings, it.message)
        )
      }

    val savedSettings = response.settings.toForm()
    return uiState.copy(
      apiState = EmailManagementApiState.Success(EmailManagementOperation.SaveSettings),
      savedSettings = savedSettings,
      draftSettings = savedSettings,
    )
  }

  suspend fun sendTestEmail(uiState: EmailManagementUiState): EmailManagementUiState {
    api.sendTestEmail().getOrElse {
      return uiState.copy(
        apiState = EmailManagementApiState.Failure(EmailManagementOperation.SendTest, it.message)
      )
    }
    return uiState.copy(
      apiState = EmailManagementApiState.Success(EmailManagementOperation.SendTest)
    )
  }

  suspend fun loadUsers(uiState: EmailManagementUiState): EmailManagementUiState {
    val users =
      loadUsersInternal().getOrElse {
        return uiState.copy(
          apiState = EmailManagementApiState.Failure(EmailManagementOperation.LoadUsers, it.message)
        )
      }
    return uiState.copy(
      apiState = EmailManagementApiState.Idle,
      users = users,
      hasLoadedUsers = true,
    )
  }

  suspend fun updateDevices(
    uiState: EmailManagementUiState,
    devices: List<EreaderDeviceItem>,
    mutation: DeviceMutation,
  ): EmailManagementUiState {
    val response =
      api
        .updateEreaderDevices(
          UpdateEreaderDevicesRequest(devices.map(EreaderDeviceItem::toNetwork))
        )
        .getOrElse {
          return uiState.copy(
            apiState =
              EmailManagementApiState.Failure(EmailManagementOperation.UpdateDevices, it.message)
          )
        }

    return uiState.copy(
      apiState = EmailManagementApiState.Success(EmailManagementOperation.UpdateDevices, mutation),
      devices = response.ereaderDevices.map(EreaderDevice::toItem),
      editorState = DeviceEditorState(),
      pendingDeleteDeviceName = null,
    )
  }

  private suspend fun loadUsersInternal(): Result<List<UserOption>> {
    val result = userRepo.remote(include = null)
    val response = result.getOrElse {
      return Result.failure(it)
    }
    return Result.success(
      response.users.sortedBy { it.createdAt }.map { user -> UserOption(user.id, user.username) }
    )
  }
}

private fun EmailSettings.toForm(): EmailSettingsForm =
  EmailSettingsForm(
    host = host.orEmpty(),
    port = port.toString(),
    secure = secure,
    rejectUnauthorized = rejectUnauthorized,
    user = user.orEmpty(),
    pass = pass.orEmpty(),
    fromAddress = fromAddress.orEmpty(),
    testAddress = testAddress.orEmpty(),
  )

private fun EreaderDevice.toItem(): EreaderDeviceItem =
  EreaderDeviceItem(
    name = name,
    email = email,
    availabilityOption = DeviceAvailabilityOption.fromWireValue(availabilityOption),
    users = users,
  )

private fun EmailSettingsForm.toRequest(): UpdateEmailSettingsRequest =
  UpdateEmailSettingsRequest(
    host = host,
    port = port.toIntOrNull(),
    secure = secure,
    rejectUnauthorized = rejectUnauthorized,
    user = user,
    pass = pass,
    testAddress = testAddress,
    fromAddress = fromAddress,
  )
