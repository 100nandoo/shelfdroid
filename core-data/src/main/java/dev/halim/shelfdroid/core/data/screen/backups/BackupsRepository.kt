package dev.halim.shelfdroid.core.data.screen.backups

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.UpdateServerSettingsRequest
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class BackupsRepository
@Inject
constructor(private val api: ApiService, private val backupsMapper: BackupsMapper) {

  suspend fun backups(): BackupsUiState {
    val backupsResult =
      api.backups().getOrElse {
        return BackupsUiState(state = GenericState.Failure(it.message))
      }

    val settingsResult =
      api.authorize().getOrElse {
        return BackupsUiState(
          state = GenericState.Success,
          backups = backupsMapper.items(backupsResult),
          backupLocation = backupsResult.backupLocation ?: "",
        )
      }

    val uiState =
      BackupsUiState(
        state = GenericState.Success,
        backups = backupsMapper.items(backupsResult),
        backupLocation = backupsResult.backupLocation ?: settingsResult.serverSettings.backupPath,
      )

    return backupsMapper.applySettings(uiState, settingsResult.serverSettings)
  }

  suspend fun createBackup(uiState: BackupsUiState): BackupsUiState {
    val response =
      api.createBackup().getOrElse {
        return uiState.copy(apiState = BackupsApiState.CreateFailure(it.message))
      }

    return uiState.copy(
      apiState = BackupsApiState.CreateSuccess,
      backups = backupsMapper.items(response),
    )
  }

  suspend fun deleteBackup(backupId: String, uiState: BackupsUiState): BackupsUiState {
    val response =
      api.deleteBackup(backupId).getOrElse {
        return uiState.copy(apiState = BackupsApiState.DeleteFailure(it.message))
      }

    return uiState.copy(
      apiState = BackupsApiState.DeleteSuccess,
      backups = backupsMapper.items(response),
    )
  }

  suspend fun restoreBackup(backupId: String, uiState: BackupsUiState): BackupsUiState {
    api.applyBackup(backupId).getOrElse {
      return uiState.copy(apiState = BackupsApiState.RestoreFailure(it.message))
    }

    return uiState.copy(apiState = BackupsApiState.RestoreSuccess)
  }

  suspend fun updateBackupLocation(path: String, uiState: BackupsUiState): BackupsUiState =
    updateSettings(uiState, UpdateServerSettingsRequest(backupPath = path))

  suspend fun updateBackupSchedule(schedule: String, uiState: BackupsUiState): BackupsUiState =
    updateSettings(uiState, UpdateServerSettingsRequest(backupSchedule = schedule))

  suspend fun updateBackupsToKeep(count: Int, uiState: BackupsUiState): BackupsUiState =
    updateSettings(uiState, UpdateServerSettingsRequest(backupsToKeep = count))

  suspend fun updateMaxBackupSize(sizeGb: Int, uiState: BackupsUiState): BackupsUiState =
    updateSettings(uiState, UpdateServerSettingsRequest(maxBackupSize = sizeGb))

  private suspend fun updateSettings(
    uiState: BackupsUiState,
    request: UpdateServerSettingsRequest,
  ): BackupsUiState {
    val response =
      api.updateSettings(request).getOrElse {
        return uiState.copy(apiState = BackupsApiState.SettingsFailure(it.message))
      }

    return backupsMapper.applySettings(
      uiState.copy(apiState = BackupsApiState.SettingsSuccess),
      response.serverSettings,
    )
  }

  suspend fun uploadBackup(
    uiState: BackupsUiState,
    filename: String,
    bytes: ByteArray,
  ): BackupsUiState {
    val requestBody = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
    val file = MultipartBody.Part.createFormData("file", filename, requestBody)

    api.uploadBackup(file).getOrElse {
      return uiState.copy(apiState = BackupsApiState.UploadFailure(it.message))
    }

    // Refresh backups list after upload
    val backupsResult =
      api.backups().getOrElse {
        return uiState.copy(apiState = BackupsApiState.UploadSuccess)
      }

    return uiState.copy(
      apiState = BackupsApiState.UploadSuccess,
      backups = backupsMapper.items(backupsResult),
    )
  }
}
