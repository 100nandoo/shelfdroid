@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.episode

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.components.PlayButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun EpisodeScreen(
  viewModel: EpisodeViewModel = hiltViewModel(),
  onPlayClicked: (String, String) -> Unit,
) {

  val uiState by viewModel.uiState.collectAsState()
  if (uiState.state == GenericState.Success) {
    EpisodeScreenContent(
      itemId = viewModel.itemId,
      episodeId = viewModel.episodeId,
      title = uiState.title,
      podcast = uiState.podcast,
      description = uiState.description,
      onPlayClicked = { onPlayClicked(viewModel.itemId, viewModel.episodeId) },
    )
  }
}

@Composable
fun EpisodeScreenContent(
  itemId: String = Defaults.BOOK_ID,
  episodeId: String = Defaults.EPISODE_ID,
  title: String = Defaults.EPISODE_TITLE,
  podcast: String = Defaults.EPISODE_PODCAST,
  description: String = Defaults.EPISODE_DESCRIPTION,
  onPlayClicked: () -> Unit = {},
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Column(
        modifier =
          Modifier.mySharedBound(Animations.Companion.Episode.containerKey(episodeId))
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Bottom,
      ) {
        Text(
          modifier =
            Modifier.mySharedBound(Animations.Companion.Episode.titleKey(episodeId, title)),
          text = title,
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          modifier = Modifier.mySharedBound(Animations.authorKey(itemId, podcast)),
          text = podcast,
          style = MaterialTheme.typography.titleMedium,
          color = Color.Gray,
          textAlign = TextAlign.Center,
        )
        ExpandShrinkText(text = description, maxLines = 3, expanded = true)
        PlayButton { onPlayClicked() }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { _ ->
        CompositionLocalProvider(
          LocalSharedTransitionScope provides this@SharedTransitionLayout,
          LocalAnimatedContentScope provides this@AnimatedContent,
        ) {
          EpisodeScreenContent()
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { _ ->
        CompositionLocalProvider(
          LocalSharedTransitionScope provides this@SharedTransitionLayout,
          LocalAnimatedContentScope provides this@AnimatedContent,
        ) {
          EpisodeScreenContent()
        }
      }
    }
  }
}
