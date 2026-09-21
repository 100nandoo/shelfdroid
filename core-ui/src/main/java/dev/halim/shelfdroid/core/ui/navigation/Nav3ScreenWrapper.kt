package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope

@Composable
fun Nav3ScreenWrapper(
  sharedTransitionScope: SharedTransitionScope,
  content: @Composable () -> Unit,
) {
  val animatedContentScope = LocalNavAnimatedContentScope.current
  CompositionLocalProvider(
    LocalSharedTransitionScope provides sharedTransitionScope,
    LocalAnimatedContentScope provides animatedContentScope,
  ) {
    content()
  }
}
