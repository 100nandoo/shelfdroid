@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.screen.player.MultipleButtonState
import dev.halim.shelfdroid.core.data.screen.player.PlayerState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.screen.MainActivity

@Composable
fun Player(
  sharedTransitionScope: SharedTransitionScope,
  viewModel: PlayerViewModel = hiltViewModel(),
) {
  val uiState = viewModel.uiState.collectAsState()
  AnimatedContent(targetState = uiState.value.state, label = "PlayerTransition") { targetState ->
    CompositionLocalProvider(
      LocalSharedTransitionScope provides sharedTransitionScope,
      LocalAnimatedContentScope provides this@AnimatedContent,
    ) {
      val onSwipeUp = {
        when (targetState) {
          is PlayerState.Hidden,
          PlayerState.TempHidden,
          PlayerState.Big -> {}
          PlayerState.Small -> viewModel.onEvent(PlayerEvent.Big)
        }
      }
      val onSwipeDown = {
        when (targetState) {
          PlayerState.Small -> {
            viewModel.onEvent(PlayerEvent.Hidden)
          }
          PlayerState.Big -> viewModel.onEvent(PlayerEvent.Small)
          is PlayerState.Hidden,
          PlayerState.TempHidden -> {}
        }
      }
      Column {
        when (targetState) {
          PlayerState.Small -> {
            val context = LocalContext.current
            LaunchedEffect(context) {
              if (context is MainActivity) {
                context.initMediaController()
              }
            }
            SmallPlayerContent(
              id = uiState.value.id,
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.playbackProgress.progress,
              multipleButtonState = uiState.value.multipleButtonState,
              onSeekBackClick = { viewModel.onEvent(PlayerEvent.SeekBack) },
              onSeekForwardClick = { viewModel.onEvent(PlayerEvent.SeekForward) },
              onPlayPauseClick = { viewModel.onEvent(PlayerEvent.PlayPause) },
              onClicked = { viewModel.onEvent(PlayerEvent.Big) },
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
            )
          }
          PlayerState.Big -> {
            BackHandler(enabled = true) { viewModel.onEvent(PlayerEvent.Small) }
            BigPlayerContent(
              id = uiState.value.id,
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.playbackProgress,
              advancedControl = uiState.value.advancedControl,
              chapters = uiState.value.playerChapters,
              bookmarks = uiState.value.playerBookmarks,
              newBookmarkTime = uiState.value.newBookmarkTime,
              currentChapter = uiState.value.currentChapter,
              multipleButtonState = uiState.value.multipleButtonState,
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
              onEvent = { event -> viewModel.onEvent(event) },
            )
          }
          is PlayerState.Hidden,
          PlayerState.TempHidden -> {}
        }
      }
    }
  }
}

@Composable
fun PlayPauseButton(
  onClick: () -> Unit,
  multipleButtonState: MultipleButtonState,
  id: String,
  size: Int = 48,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      val icon = if (multipleButtonState.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause
      val contentDescription = if (multipleButtonState.showPlay) "Play" else "Pause"
      MyIconButton(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.playKey(id)),
        icon = icon,
        size = size,
        contentDescription = contentDescription,
        onClick = { onClick() },
        enabled = multipleButtonState.playPauseEnabled,
      )
    }
  }
}

@Composable
fun SeekBackButton(onClick: () -> Unit, state: MultipleButtonState, id: String, size: Int = 48) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      MyIconButton(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.seekBackKey(id)),
        icon = Icons.Default.Replay10,
        size = size,
        contentDescription = "Seek Back",
        onClick = { onClick() },
        enabled = state.seekBackEnabled,
      )
    }
  }
}

@Composable
fun SeekForwardButton(onClick: () -> Unit, state: MultipleButtonState, id: String, size: Int = 48) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      MyIconButton(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.seekForwardKey(id)),
        icon = Icons.Default.Forward10,
        size = size,
        contentDescription = "Seek Forward",
        onClick = { onClick() },
        enabled = state.seekForwardEnabled,
      )
    }
  }
}
