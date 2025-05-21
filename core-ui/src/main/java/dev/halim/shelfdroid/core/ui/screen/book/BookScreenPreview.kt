@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.book

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun BookScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        BookScreenContent(
          sharedTransitionScope = this@SharedTransitionLayout,
          animatedContentScope = this@AnimatedContent,
          onPlayClicked = {},
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun BookScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        BookScreenContent(
          sharedTransitionScope = this@SharedTransitionLayout,
          animatedContentScope = this@AnimatedContent,
          onPlayClicked = {},
        )
      }
    }
  }
}
