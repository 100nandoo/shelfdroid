package dev.halim.shelfdroid.core.ui.screen.backups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.backups.BackupsApiState
import dev.halim.shelfdroid.core.data.screen.backups.BackupsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextLabelMedium
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun BackupsScreen(
  viewModel: BackupsViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val filePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      uri?.let {
        val bytes = context.contentResolver.openInputStream(it)?.use { s -> s.readBytes() }
        val filename = it.lastPathSegment ?: "backup.audiobookshelf"
        if (bytes != null) viewModel.onEvent(BackupsEvent.UploadBackup(filename, bytes))
      }
    }

  HandleBackupsSnackbar(uiState, snackbarHostState)
  BackupsContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
    onDownload = { backup -> viewModel.backupDownloader.download(backup) },
    onUpload = { filePicker.launch("*/*") },
  )
}

@Composable
private fun BackupsContent(
  uiState: BackupsUiState = BackupsUiState(),
  onEvent: (BackupsEvent) -> Unit = {},
  onDownload: (BackupsUiState.BackupItem) -> Unit = {},
  onUpload: () -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(
      uiState.state is GenericState.Loading || uiState.apiState is BackupsApiState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    val state = uiState.state
    if (state is GenericState.Failure) {
      GenericMessageScreen(state.errorMessage ?: "")
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Bottom,
      reverseLayout = true,
    ) {
      item(key = "actions") {
        ActionButtons(modifier = Modifier.animateItem(), onUpload = onUpload, onEvent = onEvent)
      }

      if (uiState.backups.isEmpty() && uiState.state is GenericState.Success) {
        item(key = "empty") {
          Box(modifier = Modifier.animateItem()) {
            GenericMessageScreen(
              stringResource(R.string.empty_type, stringResource(R.string.backups))
            )
          }
        }
      }

      items(uiState.backups, key = { it.id }) { backup ->
        Column(modifier = Modifier.animateItem()) {
          HorizontalDivider()
          BackupItem(backup = backup, onEvent = onEvent, onDownload = onDownload)
        }
      }

      item(key = "settings") {
        Column(modifier = Modifier.animateItem()) {
          BackupSettingsSection(uiState = uiState, onEvent = onEvent)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun BackupSettingsSection(uiState: BackupsUiState, onEvent: (BackupsEvent) -> Unit) {
  val focusManager = LocalFocusManager.current

  // Local states — reset when uiState changes (after successful save)
  var locationValue by remember(uiState.backupLocation) { mutableStateOf(uiState.backupLocation) }
  var scheduleValue by remember(uiState.backupSchedule) { mutableStateOf(uiState.backupSchedule) }
  var keepValue by
    remember(uiState.backupsToKeep) { mutableStateOf(uiState.backupsToKeep.toString()) }
  var maxSizeValue by
    remember(uiState.maxBackupSize) { mutableStateOf(uiState.maxBackupSize.toString()) }

  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    OutlinedTextField(
      value = locationValue,
      onValueChange = { locationValue = it },
      label = { Text(stringResource(R.string.backup_location)) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions =
        KeyboardActions(
          onDone = {
            focusManager.clearFocus()
            onEvent(BackupsEvent.UpdateBackupLocation(locationValue))
          }
        ),
    )

    MySwitch(
      modifier = Modifier.padding(top = 16.dp, start = 4.dp),
      title = stringResource(R.string.automatic_backups),
      checked = uiState.autoBackupEnabled,
      onCheckedChange = { onEvent(BackupsEvent.UpdateAutoBackup(it)) },
      contentDescription = stringResource(R.string.automatic_backups),
    )

    // Schedule + next backup date (only when auto backup enabled)
    AnimatedVisibility(visible = uiState.autoBackupEnabled) {
      Column {
        OutlinedTextField(
          value = scheduleValue,
          onValueChange = { scheduleValue = it },
          label = { Text(stringResource(R.string.backup_schedule)) },
          placeholder = {
            Text(
              stringResource(R.string.cron_expression_hint),
              style = MaterialTheme.typography.labelSmall,
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions =
            KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                onEvent(BackupsEvent.UpdateSchedule(scheduleValue))
              }
            ),
        )
        if (uiState.nextBackupDate.isNotBlank()) {
          TextLabelMedium(
            text = stringResource(R.string.next_backup_date, uiState.nextBackupDate),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp, top = 4.dp),
          )
        }
      }
    }

    // Backups to Keep
    OutlinedTextField(
      value = keepValue,
      onValueChange = { keepValue = it.filter { c -> c.isDigit() } },
      label = { Text(stringResource(R.string.backups_to_keep)) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
      keyboardActions =
        KeyboardActions(
          onDone = {
            focusManager.clearFocus()
            onEvent(
              BackupsEvent.UpdateBackupsToKeep(keepValue.toIntOrNull() ?: uiState.backupsToKeep)
            )
          }
        ),
    )

    // Max Backup Size
    OutlinedTextField(
      value = maxSizeValue,
      onValueChange = { maxSizeValue = it.filter { c -> c.isDigit() } },
      label = { Text(stringResource(R.string.max_backup_size)) },
      placeholder = {
        Text(
          stringResource(R.string.max_backup_size_hint),
          style = MaterialTheme.typography.labelSmall,
        )
      },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
      keyboardActions =
        KeyboardActions(
          onDone = {
            focusManager.clearFocus()
            onEvent(
              BackupsEvent.UpdateMaxBackupSize(maxSizeValue.toIntOrNull() ?: uiState.maxBackupSize)
            )
          }
        ),
    )

    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun ActionButtons(
  modifier: Modifier = Modifier,
  onUpload: () -> Unit,
  onEvent: (BackupsEvent) -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    TextButton(onClick = onUpload, modifier = Modifier.weight(1f)) {
      Text(text = stringResource(R.string.upload_backup))
    }
    TextButton(onClick = { onEvent(BackupsEvent.CreateBackup) }, modifier = Modifier.weight(1f)) {
      Text(text = stringResource(R.string.create_backup))
    }
  }
}

@Composable
private fun HandleBackupsSnackbar(uiState: BackupsUiState, snackbarHostState: SnackbarHostState) {
  val createSuccess = stringResource(R.string.backup_created)
  val createError = stringResource(R.string.create_backup_failed)
  val deleteSuccess = stringResource(R.string.backup_deleted)
  val deleteError = stringResource(R.string.delete_backup_failed)
  val restoreSuccess = stringResource(R.string.backup_restored)
  val restoreError = stringResource(R.string.restore_backup_failed)
  val settingsSuccess = stringResource(R.string.settings_saved)
  val settingsError = stringResource(R.string.settings_save_failed)
  val uploadSuccess = stringResource(R.string.backup_uploaded)
  val uploadError = stringResource(R.string.upload_backup_failed)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      BackupsApiState.CreateSuccess -> snackbarHostState.showSuccessSnackbar(createSuccess)
      is BackupsApiState.CreateFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: createError)
      BackupsApiState.DeleteSuccess -> snackbarHostState.showSuccessSnackbar(deleteSuccess)
      is BackupsApiState.DeleteFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: deleteError)
      BackupsApiState.RestoreSuccess -> snackbarHostState.showSuccessSnackbar(restoreSuccess)
      is BackupsApiState.RestoreFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: restoreError)
      BackupsApiState.SettingsSuccess -> snackbarHostState.showSuccessSnackbar(settingsSuccess)
      is BackupsApiState.SettingsFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: settingsError)
      BackupsApiState.UploadSuccess -> snackbarHostState.showSuccessSnackbar(uploadSuccess)
      is BackupsApiState.UploadFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: uploadError)
      else -> Unit
    }
  }
}

@ShelfDroidPreview
@Composable
fun BackupsScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    BackupsContent(uiState = Defaults.BACKUPS_UI_STATE)
  }
}
