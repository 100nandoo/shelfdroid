package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.podcast.DownloadState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.ui.R

@Composable
fun DownloadButton(episode: Episode, onEvent: (PodcastEvent) -> Unit) {
  val isDownloading = episode.downloadState == DownloadState.Downloading
  val isDownloaded = episode.downloadState == DownloadState.Completed

  val buttonColors =
    if (isDownloaded) IconButtonDefaults.filledIconButtonColors()
    else IconButtonDefaults.filledTonalIconButtonColors()

  FilledTonalIconButton(
    onClick = {
      if (!isDownloaded) {
        onEvent(PodcastEvent.Download(episode))
      } else {
        onEvent(PodcastEvent.DeleteDownload(episode))
      }
    },
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
