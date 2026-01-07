package dev.halim.shelfdroid.core.ui.screen.addepisode

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.addepisode.Episode
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.components.CoverWithTitle
import dev.halim.shelfdroid.core.ui.extensions.enable
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun AddEpisodeScreen(viewModel: AddEpisodeViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  AddEpisodeScreenContent(
    itemId = viewModel.id,
    state = uiState.state,
    title = uiState.title,
    author = uiState.author,
    cover = uiState.cover,
    episodes = uiState.episodes,
    onEvent = viewModel::onEvent,
  )
}

@Composable
private fun AddEpisodeScreenContent(
  itemId: String = "",
  state: GenericState = GenericState.Success,
  title: String = Defaults.TITLE,
  author: String = Defaults.AUTHOR_NAME,
  cover: String = Defaults.IMAGE_URL,
  episodes: List<Episode> = Defaults.ADD_EPISODE_EPISODES,
  onEvent: (AddEpisodeEvent) -> Unit = {},
) {
  AnimatedVisibility(state is GenericState.Loading) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }
  AnimatedVisibility(state is GenericState.Success) {
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      item {
        CoverWithTitle(
          cover = cover,
          coverAnimationKey = Animations.coverKey(itemId),
          title = title,
          titleAnimationKey = Animations.titleKey(itemId, title),
          subtitle = author,
          subtitleAnimationKey = Animations.authorKey(itemId, author),
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      items(items = episodes, key = { it.url }) { episode ->
        AddEpisodeItem(episode) { onEvent(AddEpisodeEvent.CheckEpisode(episode.url, it)) }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      }
    }
  }
}

@Composable
private fun AddEpisodeItem(episode: Episode, onCheckedChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    val checked =
      episode.state == AddEpisodeDownloadState.Downloaded ||
        episode.state == AddEpisodeDownloadState.ToBeDownloaded

    val enabled = episode.state != AddEpisodeDownloadState.Downloaded

    Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    Column(modifier = Modifier.weight(1f)) {
      Text(episode.title, maxLines = 2, color = MaterialTheme.colorScheme.onSurface.enable(enabled))
      Text(
        episode.description,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurfaceVariant.enable(enabled),
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun AddEpisodeScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { AddEpisodeScreenContent() }
}
