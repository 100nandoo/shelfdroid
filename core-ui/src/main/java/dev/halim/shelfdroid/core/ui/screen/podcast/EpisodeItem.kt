package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DownloadButton
import dev.halim.shelfdroid.core.ui.components.VisibilityUp
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun EpisodeItem(
  itemId: String,
  episode: Episode,
  isSelected: Boolean,
  canDelete: Boolean,
  onEvent: (PodcastEvent) -> Unit,
  onEpisodeClicked: (String, String) -> Unit,
  onPlayClicked: (String, String, Boolean) -> Unit,
  snackbarHostState: SnackbarHostState,
  isSelectionMode: Boolean,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.mySharedBound(Animations.Companion.Episode.containerKey(episode.episodeId))
        .fillMaxWidth()
        .combinedClickable(
          onLongClick = {
            if (isSelectionMode.not() && canDelete) {
              onEvent(PodcastEvent.SelectionMode(true, episode.episodeId))
            }
          },
          onClick = {
            if (isSelectionMode.not()) {
              onEpisodeClicked(itemId, episode.episodeId)
            } else {
              onEvent(PodcastEvent.SelectItem(episode.episodeId))
            }
          },
        )
        .padding(vertical = 8.dp),
  ) {
    AnimatedVisibility(isSelectionMode) {
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onEvent(PodcastEvent.SelectItem(episode.episodeId)) },
      )
    }
    if (isSelectionMode.not()) {
      Spacer(Modifier.width(16.dp))
    }
    Column {
      Text(
        text = episode.title,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
          Modifier.mySharedElement(Animations.Companion.Episode.titleKey(episode.episodeId))
            .alpha(episode.isFinished.not().enableAlpha())
            .padding(end = 16.dp),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.alpha(episode.isFinished.not().enableAlpha()).padding(end = 16.dp)) {
        Column(modifier = Modifier.weight(1f).height(40.dp)) {
          Text(
            modifier =
              Modifier.mySharedElement(
                Animations.Companion.Episode.publishedAtKey(episode.episodeId)
              ),
            text = episode.publishedAt,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.weight(1f))
          VisibilityUp(episode.isFinished.not() || episode.isPlaying) {
            val currentProgress =
              when {
                !episode.isFinished -> episode.progress
                else -> 0f
              }
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
              progress = { currentProgress },
              drawStopIndicator = {},
            )
          }
        }
        val checkButtonColors =
          if (episode.isFinished) IconButtonDefaults.filledIconButtonColors()
          else IconButtonDefaults.filledTonalIconButtonColors()
        FilledTonalIconButton(
          onClick = { onEvent(PodcastEvent.ToggleIsFinished(episode)) },
          colors = checkButtonColors,
        ) {
          Icon(Icons.Default.Check, contentDescription = stringResource(R.string.mark_as_finished))
        }
        DownloadButton(
          episode.download.state,
          snackbarHostState,
          {
            onEvent(PodcastEvent.Download(episode.download.id, episode.download.url, episode.title))
          },
          { onEvent(PodcastEvent.DeleteDownload(episode.download.id)) },
        )

        FilledTonalIconButton(
          onClick = {
            onPlayClicked(itemId, episode.episodeId, episode.download.state.isDownloaded())
          }
        ) {
          val icon = if (episode.isPlaying.not()) Icons.Default.PlayArrow else Icons.Default.Pause
          val contentDescription =
            if (episode.isPlaying.not()) stringResource(R.string.play)
            else stringResource(R.string.pause)
          Icon(icon, contentDescription)
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeItemPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    LazyColumn(reverseLayout = true) {
      item { Spacer(modifier = Modifier.height(12.dp)) }
      Defaults.EPISODES.forEach { episode ->
        item {
          EpisodeItem(
            "",
            episode,
            true,
            true,
            {},
            { _, _ -> },
            { _, _, _ -> },
            SnackbarHostState(),
            true,
          )
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeItemDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    LazyColumn(reverseLayout = true) {
      item { Spacer(modifier = Modifier.height(12.dp)) }
      Defaults.EPISODES.forEach { episode ->
        item {
          EpisodeItem(
            "",
            episode,
            false,
            false,
            {},
            { _, _ -> },
            { _, _, _ -> },
            SnackbarHostState(),
            false,
          )
        }
      }
    }
  }
}
