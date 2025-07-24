@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import ItemDetail
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.player.ExoState
import dev.halim.shelfdroid.core.data.screen.player.PlaybackProgress
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.InitMediaControllerIfMainActivity
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun PodcastScreen(
  viewModel: PodcastViewModel = hiltViewModel(),
  playerViewModel: PlayerViewModel,
  onEpisodeClicked: (String, String) -> Unit,
  onPlayClicked: (String, String) -> Unit,
) {
  InitMediaControllerIfMainActivity()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
  if (uiState.state == GenericState.Success) {
    PodcastScreenContent(
      uiState = uiState,
      currentEpisodeId = playerUiState.episodeId,
      exoState = playerUiState.exoState,
      playbackProgress = playerUiState.playbackProgress,
      id = viewModel.id,
      onEvent = viewModel::onEvent,
      onEpisodeClicked = onEpisodeClicked,
      onPlayClicked = onPlayClicked,
    )
  }
}

@Composable
fun PodcastScreenContent(
  uiState: PodcastUiState =
    PodcastUiState(
      state = GenericState.Success,
      title = Defaults.TITLE,
      author = Defaults.AUTHOR_NAME,
      cover = Defaults.IMAGE_URL,
      description = Defaults.DESCRIPTION,
      episodes = Defaults.EPISODES,
    ),
  currentEpisodeId: String = Defaults.EPISODE_ID,
  exoState: ExoState = ExoState.Pause,
  playbackProgress: PlaybackProgress = PlaybackProgress(),
  id: String = Defaults.BOOK_ID,
  onEvent: (PodcastEvent) -> Unit = {},
  onEpisodeClicked: (String, String) -> Unit = { _, _ -> },
  onPlayClicked: (String, String) -> Unit = { _, _ -> },
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      LazyColumn(
        modifier = Modifier.mySharedBound(Animations.containerKey(id)).fillMaxSize(),
        reverseLayout = true,
      ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        items(uiState.episodes) { episode ->
          val isPlaying = currentEpisodeId == episode.id && exoState == ExoState.Playing
          EpisodeItem(
            id,
            episode,
            onEvent,
            onEpisodeClicked,
            onPlayClicked,
            isPlaying,
            playbackProgress,
          )
          HorizontalDivider()
        }
        item {
          Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.episodes),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Left,
          )
          Spacer(modifier = Modifier.height(16.dp))

          ExpandShrinkText(Modifier.padding(horizontal = 16.dp), uiState.description)

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp),
          ) {
            ItemDetail(id, uiState.cover, uiState.title, uiState.author)
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { PodcastScreenContent(onPlayClicked = { _, _ -> }) }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { PodcastScreenContent(onPlayClicked = { _, _ -> }) }
}
