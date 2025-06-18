@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import javax.inject.Inject

class Animations @Inject constructor() {
  companion object {
    // between home and book/podcast
    fun containerKey(id: String) = "container_$id"

    fun titleKey(id: String, title: String) = "${id}_title_$title"

    fun authorKey(id: String, author: String) = "${id}_author_$author"

    fun coverKey(id: String) = "cover_$id"

    // between podcast and episode
    object Episode {
      fun titleKey(id: String, title: String) = "${id}_episode_$title"

      fun containerKey(id: String) = "container_$id"
    }

    object Player {
      fun containerKey(id: String) = "player_container_$id"

      fun titleKey(title: String) = "player_title_$title"

      fun coverKey(id: String) = "player_cover_$id"

      fun progressKey(id: String) = "player_progress_$id"

      fun authorKey(author: String) = "player_author_$author"

      fun playKey(id: String) = "player_play_$id"

      fun seekBackKey(id: String) = "player_seek_back_$id"

      fun seekForwardKey(id: String) = "player_seek_forward_$id"
    }
  }
}

val LocalSharedTransitionScope =
  staticCompositionLocalOf<SharedTransitionScope> { error("SharedTransitionScope not provided") }

val LocalAnimatedContentScope =
  staticCompositionLocalOf<AnimatedContentScope> { error("AnimatedContentScope not provided") }

context(SharedTransitionScope, AnimatedContentScope)
@Composable
fun Modifier.mySharedBound(id: String) =
  this.sharedBounds(
    rememberSharedContentState(key = id),
    animatedVisibilityScope = this@AnimatedContentScope,
  )

context(SharedTransitionScope, AnimatedContentScope)
@Composable
fun Modifier.mySharedElement(id: String, overlayClip: OverlayClip) =
  this.sharedElement(
    rememberSharedContentState(key = id),
    animatedVisibilityScope = this@AnimatedContentScope,
    clipInOverlayDuringTransition = overlayClip,
  )
