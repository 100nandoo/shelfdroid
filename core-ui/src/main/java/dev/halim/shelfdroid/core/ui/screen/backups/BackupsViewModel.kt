package dev.halim.shelfdroid.core.ui.screen.backups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.backups.BackupDownloader
import dev.halim.shelfdroid.core.data.screen.backups.BackupsApiState
import dev.halim.shelfdroid.core.data.screen.backups.BackupsRepository
import dev.halim.shelfdroid.core.data.screen.backups.BackupsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BackupsViewModel
@Inject
constructor(private val repository: BackupsRepository, val backupDownloader: BackupDownloader) :
  ViewModel() {

  private val _uiState = MutableStateFlow(BackupsUiState())
  val uiState: StateFlow<BackupsUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BackupsUiState())

  fun onEvent(event: BackupsEvent) {
    when (event) {
      BackupsEvent.CreateBackup -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.createBackup(it) }
        }
      }
      is BackupsEvent.DeleteBackup -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.deleteBackup(event.backupId, it) }
        }
      }
      is BackupsEvent.RestoreBackup -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          val result = repository.restoreBackup(event.backupId, _uiState.value)
          if (result.apiState is BackupsApiState.RestoreSuccess) {
            _uiState.update { repository.backups().copy(apiState = BackupsApiState.RestoreSuccess) }
          } else {
            _uiState.update { result }
          }
        }
      }
      is BackupsEvent.UpdateBackupLocation -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.updateBackupLocation(event.path, it) }
        }
      }
      is BackupsEvent.UpdateAutoBackup -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          val schedule =
            if (event.enabled) _uiState.value.backupSchedule.ifBlank { "0 2 * * *" } else ""
          _uiState.update { repository.updateBackupSchedule(schedule, it) }
        }
      }
      is BackupsEvent.UpdateSchedule -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.updateBackupSchedule(event.cronExpression, it) }
        }
      }
      is BackupsEvent.UpdateBackupsToKeep -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.updateBackupsToKeep(event.count, it) }
        }
      }
      is BackupsEvent.UpdateMaxBackupSize -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.updateMaxBackupSize(event.sizeGb, it) }
        }
      }
      is BackupsEvent.UploadBackup -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = BackupsApiState.Loading) }
          _uiState.update { repository.uploadBackup(it, event.filename, event.bytes) }
        }
      }
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.backups() } }
  }
}

sealed interface BackupsEvent {
  data object CreateBackup : BackupsEvent

  data class DeleteBackup(val backupId: String) : BackupsEvent

  data class RestoreBackup(val backupId: String) : BackupsEvent

  data class UpdateBackupLocation(val path: String) : BackupsEvent

  data class UpdateAutoBackup(val enabled: Boolean) : BackupsEvent

  data class UpdateSchedule(val cronExpression: String) : BackupsEvent

  data class UpdateBackupsToKeep(val count: Int) : BackupsEvent

  data class UpdateMaxBackupSize(val sizeGb: Int) : BackupsEvent

  data class UploadBackup(val filename: String, val bytes: ByteArray) : BackupsEvent
}
