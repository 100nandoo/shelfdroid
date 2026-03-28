package dev.halim.shelfdroid.core.data.screen.backups

import dev.halim.shelfdroid.core.data.GenericState

data class BackupsUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: BackupsApiState = BackupsApiState.Idle,
  val backups: List<BackupItem> = emptyList(),
  // settings
  val backupLocation: String = "",
  val autoBackupEnabled: Boolean = false,
  val backupSchedule: String = "",
  val nextBackupDate: String = "",
  val backupsToKeep: Int = 2,
  val maxBackupSize: Int = 1,
) {

  data class BackupItem(
    val id: String,
    val filename: String,
    val fileSize: String,
    val createdAt: String,
    val serverVersion: String,
    val downloadUrl: String,
  )
}

sealed interface BackupsApiState {
  data object Idle : BackupsApiState

  data object Loading : BackupsApiState

  data object CreateSuccess : BackupsApiState

  data class CreateFailure(val message: String?) : BackupsApiState

  data object DeleteSuccess : BackupsApiState

  data class DeleteFailure(val message: String?) : BackupsApiState

  data object RestoreSuccess : BackupsApiState

  data class RestoreFailure(val message: String?) : BackupsApiState

  data object SettingsSuccess : BackupsApiState

  data class SettingsFailure(val message: String?) : BackupsApiState

  data object UploadSuccess : BackupsApiState

  data class UploadFailure(val message: String?) : BackupsApiState
}
