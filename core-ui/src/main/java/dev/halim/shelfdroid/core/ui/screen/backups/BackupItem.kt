package dev.halim.shelfdroid.core.ui.screen.backups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.backups.BackupsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall

@Composable
fun BackupItem(
  backup: BackupsUiState.BackupItem,
  onEvent: (BackupsEvent) -> Unit = {},
  onDownload: (BackupsUiState.BackupItem) -> Unit = {},
) {
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showRestoreDialog by remember { mutableStateOf(false) }

  if (showDeleteDialog) {
    ConfirmDialog(
      title = stringResource(R.string.delete_backup_title),
      text = stringResource(R.string.delete_backup_confirm),
      onConfirm = {
        showDeleteDialog = false
        onEvent(BackupsEvent.DeleteBackup(backup.id))
      },
      onDismiss = { showDeleteDialog = false },
    )
  }

  if (showRestoreDialog) {
    ConfirmDialog(
      title = stringResource(R.string.restore_backup_title),
      text = stringResource(R.string.restore_backup_confirm),
      onConfirm = {
        showRestoreDialog = false
        onEvent(BackupsEvent.RestoreBackup(backup.id))
      },
      onDismiss = { showRestoreDialog = false },
    )
  }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
      TextTitleSmall(text = backup.filename, maxLines = 1, overflow = TextOverflow.Ellipsis)
      TextLabelSmall(
        text = backup.createdAt,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      TextLabelSmall(
        text = "${backup.fileSize} · v${backup.serverVersion}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    FilledTonalIconButton(onClick = { showRestoreDialog = true }) {
      Icon(
        painter = painterResource(R.drawable.refresh),
        contentDescription = stringResource(R.string.restore),
      )
    }
    FilledTonalIconButton(onClick = { onDownload(backup) }) {
      Icon(
        painter = painterResource(R.drawable.download),
        contentDescription = stringResource(R.string.download),
      )
    }
    FilledTonalIconButton(onClick = { showDeleteDialog = true }) {
      Icon(
        painter = painterResource(R.drawable.delete),
        contentDescription = stringResource(R.string.delete),
      )
    }
  }
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = title) },
    text = { Text(text = text) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}
