@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.episode

import ItemCover
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.player.ExoState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.components.PlayButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun EpisodeScreen(
  viewModel: EpisodeViewModel = hiltViewModel(),
  playerViewModel: PlayerViewModel,
  onPlayClicked: (String, String) -> Unit,
) {

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
  val isPlaying =
    viewModel.episodeId == playerUiState.episodeId && playerUiState.exoState == ExoState.Playing

  if (uiState.state == GenericState.Success) {
    EpisodeScreenContent(
      itemId = viewModel.itemId,
      episodeId = viewModel.episodeId,
      cover = uiState.cover,
      title = uiState.title,
      podcast = uiState.podcast,
      publishedAt = uiState.publishedAt,
      description = uiState.description,
      isPlaying = isPlaying,
      onPlayClicked = { onPlayClicked(viewModel.itemId, viewModel.episodeId) },
    )
  }
}

@Composable
fun EpisodeScreenContent(
  itemId: String = Defaults.BOOK_ID,
  episodeId: String = Defaults.EPISODE_ID,
  cover: String = Defaults.BOOK_COVER,
  title: String = Defaults.EPISODE_TITLE,
  podcast: String = Defaults.EPISODE_PODCAST,
  publishedAt: String = Defaults.EPISODE_PUBLISHED_AT,
  description: String = Defaults.EPISODE_DESCRIPTION,
  isPlaying: Boolean = false,
  onPlayClicked: () -> Unit = {},
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      LazyColumn(
        modifier =
          Modifier.mySharedBound(Animations.Companion.Episode.containerKey(episodeId))
            .padding(horizontal = 16.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.Bottom,
      ) {
        item { PlayButton(isPlaying = isPlaying) { onPlayClicked() } }
        item { ExpandShrinkText(text = description, maxLines = 3, expanded = true) }
        item {
          Row(Modifier.height(IntrinsicSize.Max)) {
            ItemCover(
              Modifier.weight(1f).fillMaxHeight(),
              cover = cover,
              animationKey = Animations.Companion.Episode.coverKey(itemId),
              fontSize = 10.sp,
              shape = RoundedCornerShape(4.dp),
            )

            Column(
              Modifier.weight(4f).padding(8.dp).fillMaxHeight(),
              verticalArrangement = Arrangement.Center,
            ) {
              Text(
                modifier =
                  Modifier.mySharedBound(Animations.Companion.Episode.titleKey(episodeId, title)),
                text = title,
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                modifier =
                  Modifier.mySharedElement(Animations.Companion.Episode.publishedAtKey(episodeId)),
                text = publishedAt,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { EpisodeScreenContent() }
}

@ShelfDroidPreview
@Composable
fun EpisodeScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { EpisodeScreenContent() }
}
