package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.DownloadState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.permissions.rememberNotificationPermissionHandler

@Composable
fun DownloadButton(
  downloadState: DownloadState,
  snackbarHostState: SnackbarHostState,
  onDownloadClicked: () -> Unit,
  onDeleteDownloadClicked: () -> Unit,
) {
  val isDownloading = downloadState == DownloadState.Downloading
  val isDownloaded = downloadState.isDownloaded()
  var showDeleteDialog by remember { mutableStateOf(false) }

  MyAlertDialog(
    title = stringResource(R.string.delete),
    text = stringResource(R.string.dialog_delete_text),
    showDialog = showDeleteDialog,
    confirmText = stringResource(R.string.ok),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onDeleteDownloadClicked()
      showDeleteDialog = false
    },
    onDismiss = { showDeleteDialog = false },
  )

  val downloadLogic = {
    if (!isDownloaded) {
      onDownloadClicked()
    } else {
      showDeleteDialog = true
    }
  }

  val handleNotificationPermission =
    rememberNotificationPermissionHandler(
      snackbarHostState = snackbarHostState,
      onPermissionGranted = downloadLogic,
    )

  val buttonColors =
    if (isDownloaded) IconButtonDefaults.filledIconButtonColors()
    else IconButtonDefaults.filledTonalIconButtonColors()

  FilledTonalIconButton(
    onClick = handleNotificationPermission,
    colors = buttonColors,
    enabled = !isDownloading,
  ) {
    when (downloadState) {
      DownloadState.Downloading -> {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
      }
      DownloadState.Incomplete -> {
        Icon(
          imageVector = Icons.Default.FileDownloadOff,
          contentDescription = stringResource(R.string.download_incomplete),
        )
      }
      DownloadState.Completed -> {
        Icon(
          imageVector = Icons.Default.DownloadDone,
          contentDescription = stringResource(R.string.downloaded),
        )
      }
      else -> {
        Icon(
          imageVector = Icons.Default.Download,
          contentDescription = stringResource(R.string.download),
        )
      }
    }
  }
}

@Composable
fun PlayAndDownload(
  isPlaying: Boolean,
  downloadState: DownloadState,
  snackbarHostState: SnackbarHostState,
  onPlayClicked: () -> Unit,
  onDownloadClicked: () -> Unit,
  onDeleteDownloadClicked: () -> Unit,
) {
  Row(Modifier.padding(vertical = 8.dp)) {
    PlayButton(modifier = Modifier.padding(end = 8.dp), isPlaying = isPlaying) { onPlayClicked() }
    DownloadButton(
      downloadState = downloadState,
      snackbarHostState = snackbarHostState,
      onDownloadClicked = onDownloadClicked,
      onDeleteDownloadClicked = onDeleteDownloadClicked,
    )
  }
}
