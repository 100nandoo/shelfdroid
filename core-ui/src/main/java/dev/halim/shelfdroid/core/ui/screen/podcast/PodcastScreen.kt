@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import ItemDetail
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.podcast.Episode
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

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
        modifier = Modifier.mySharedBound(Animations.containerKey(id)).fillMaxSize(),
        reverseLayout = true,
      ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        items(episodes) { episode -> EpisodeItem(episode) }
        item {
          Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = "Episodes",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Left,
          )
          Spacer(modifier = Modifier.height(16.dp))

          ExpandShrinkText(Modifier.padding(horizontal = 16.dp), description)

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp),
          ) {
            ItemDetail(id, imageUrl, title, authorName)
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        CompositionLocalProvider(
          LocalSharedTransitionScope provides this@SharedTransitionLayout,
          LocalAnimatedContentScope provides this@AnimatedContent,
        ) {
          PodcastScreenContent()
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        CompositionLocalProvider(
          LocalSharedTransitionScope provides this@SharedTransitionLayout,
          LocalAnimatedContentScope provides this@AnimatedContent,
        ) {
          PodcastScreenContent()
        }
      }
    }
  }
}
