@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.player.PlaybackProgress
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun EpisodeItem(
  itemId: String,
  episode: Episode,
  onEvent: (PodcastEvent) -> Unit,
  onEpisodeClicked: (String, String) -> Unit,
  onPlayClicked: (String, String) -> Unit,
  isPlaying: Boolean,
  playbackProgress: PlaybackProgress,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Column(
        modifier =
          Modifier.mySharedBound(Animations.Companion.Episode.containerKey(episode.id))
            .fillMaxWidth()
            .clickable { onEpisodeClicked(itemId, episode.id) }
            .padding(vertical = 8.dp)
      ) {
        Text(
          text = episode.title,
          style = MaterialTheme.typography.bodyLarge,
          modifier =
            Modifier.mySharedBound(Animations.Companion.Episode.titleKey(episode.id, episode.title))
              .padding(horizontal = 16.dp),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
          Column(modifier = Modifier.weight(1f).height(40.dp)) {
            Text(
              modifier =
                Modifier.mySharedElement(Animations.Companion.Episode.publishedAtKey(episode.id)),
              text = episode.publishedAt,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.labelMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(episode.isFinished.not() || isPlaying) {
              val progress = if (isPlaying) playbackProgress.progress else episode.progress
              LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                progress = { progress },
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
            Icon(Icons.Default.Check, contentDescription = "Mark as Finished")
          }
          FilledTonalIconButton(onClick = {}, enabled = false) {
            Icon(Icons.Default.Download, contentDescription = "Download")
          }
          FilledTonalIconButton(onClick = { onPlayClicked(itemId, episode.id) }) {
            val icon = if (isPlaying.not()) Icons.Default.PlayArrow else Icons.Default.Pause
            val contentDescription = if (isPlaying.not()) "Play" else "Pause"
            Icon(icon, contentDescription)
          }
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
        item { EpisodeItem("", episode, {}, { _, _ -> }, { _, _ -> }, false, PlaybackProgress()) }
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
        item { EpisodeItem("", episode, {}, { _, _ -> }, { _, _ -> }, false, PlaybackProgress()) }
      }
    }
  }
}
