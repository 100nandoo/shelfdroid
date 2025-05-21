@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        PodcastScreenContent(
          sharedTransitionScope = this@SharedTransitionLayout,
          animatedContentScope = this@AnimatedContent,
        )
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
        PodcastScreenContent(
          sharedTransitionScope = this@SharedTransitionLayout,
          animatedContentScope = this@AnimatedContent,
        )
      }
    }
  }
}
