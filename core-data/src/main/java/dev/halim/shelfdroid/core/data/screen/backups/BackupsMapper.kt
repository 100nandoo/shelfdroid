package dev.halim.shelfdroid.core.data.screen.backups

import dev.halim.core.network.response.BackupsResponse
import dev.halim.core.network.response.ServerSettings
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class BackupsMapper @Inject constructor(private val helper: Helper) {
  suspend fun items(response: BackupsResponse): List<BackupsUiState.BackupItem> {
    return response.backups
      .sortedByDescending { it.createdAt }
      .map { backup ->
        BackupsUiState.BackupItem(
          id = backup.id,
          filename = backup.filename,
          fileSize = helper.formatFileSize(backup.fileSize),
          createdAt = helper.toReadableDate(backup.createdAt, includeTime = true),
          serverVersion = backup.serverVersion,
          downloadUrl = helper.generateBackupDownloadUrl(backup.id),
        )
      }
  }

  fun applySettings(uiState: BackupsUiState, settings: ServerSettings): BackupsUiState {
    val autoEnabled = settings.backupSchedule.isNotBlank()
    return uiState.copy(
      backupLocation = settings.backupPath,
      autoBackupEnabled = autoEnabled,
      backupSchedule = settings.backupSchedule,
      nextBackupDate = if (autoEnabled) helper.nextCronDate(settings.backupSchedule) else "",
      backupsToKeep = settings.backupsToKeep,
      maxBackupSize = settings.maxBackupSize,
    )
  }
}
