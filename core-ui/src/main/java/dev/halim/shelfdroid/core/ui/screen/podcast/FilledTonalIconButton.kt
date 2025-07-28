package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.podcast.DownloadState
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
  val isDownloaded = downloadState == DownloadState.Completed

  val downloadLogic = {
    if (!isDownloaded) {
      onDownloadClicked()
    } else {
      onDeleteDownloadClicked()
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
    if (isDownloading) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    } else {
      Icon(
        imageVector = Icons.Default.Download,
        contentDescription = stringResource(R.string.download),
      )
    }
  }
}
