@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import ItemDetail
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.podcast.Episode
import dev.halim.shelfdroid.core.data.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.utils.toPercent

@Composable
fun PodcastScreen(viewModel: PodcastViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle(PodcastUiState())
  if (uiState.state == GenericState.Success) {

    PodcastScreenContent(
      viewModel.id,
      uiState.cover,
      uiState.title,
      uiState.author,
      uiState.description,
      uiState.episodes,
    )
  }
}

@Composable
fun PodcastScreenContent(
  id: String = Defaults.BOOK_ID,
  imageUrl: String = Defaults.IMAGE_URL,
  title: String = Defaults.TITLE,
  authorName: String = Defaults.AUTHOR_NAME,
  description: String = Defaults.DESCRIPTION,
  episodes: List<Episode> = Defaults.EPISODES,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      LazyColumn(
        modifier =
          Modifier.mySharedBound(Animations.containerKey(id))
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(episodes) { episode -> EpisodeItem(episode) }
        item {
          Text(
            text = "Episodes",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Left,
          )
          Spacer(modifier = Modifier.height(16.dp))

          Text(description)

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ItemDetail(id, imageUrl, title, authorName)
          }
        }
      }
    }
  }
}

@Composable
fun EpisodeItem(episode: Episode) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(
      text = episode.title,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row {
      Text(
        text = episode.progress.toPercent(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = episode.publishedAt,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
      progress = { episode.progress },
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.tertiaryContainer,
      trackColor = MaterialTheme.colorScheme.onTertiaryContainer,
      drawStopIndicator = {},
    )
  }
}
