@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme

@Composable
fun PreviewWrapper(content: @Composable () -> Unit = {}) {
  Column {
    ShelfDroidTheme(dynamicColor = false) { Surface { content() } }
    ShelfDroidTheme(dynamicColor = true) { Surface { content() } }
  }
}

@Composable
fun PreviewWrapper(dynamicColor: Boolean = false, content: @Composable () -> Unit = {}) {
  ShelfDroidTheme(dynamicColor = dynamicColor) {
    Surface(modifier = Modifier.fillMaxSize()) { content() }
  }
}

@Composable
fun AnimatedPreviewWrapper(content: @Composable () -> Unit = {}) {
  SharedTransitionLayout {
    AnimatedContent(targetState = Unit) { animatedContentScope ->
      CompositionLocalProvider(
        LocalSharedTransitionScope provides this@SharedTransitionLayout,
        LocalAnimatedContentScope provides this@AnimatedContent,
      ) {
        PreviewWrapper(content = content)
      }
    }
  }
}

@Composable
fun AnimatedPreviewWrapper(dynamicColor: Boolean = false, content: @Composable () -> Unit = {}) {
  SharedTransitionLayout {
    AnimatedContent(targetState = Unit) { animatedContentScope ->
      CompositionLocalProvider(
        LocalSharedTransitionScope provides this@SharedTransitionLayout,
        LocalAnimatedContentScope provides this@AnimatedContent,
      ) {
        PreviewWrapper(dynamicColor = dynamicColor, content = content)
      }
    }
  }
}
