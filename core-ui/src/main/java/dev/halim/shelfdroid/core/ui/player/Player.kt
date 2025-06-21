@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import dev.halim.shelfdroid.core.data.screen.player.PlayerState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound

@Composable
fun Player(
  sharedTransitionScope: SharedTransitionScope,
  viewModel: PlayerViewModel = hiltViewModel(),
) {
  val uiState = viewModel.uiState.collectAsState()
  val player = remember { viewModel.player }
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
            SmallPlayerContent(
              player = player,
              id = uiState.value.id,
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.progress,
              onClicked = { viewModel.onEvent(PlayerEvent.Big) },
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
            )
          }
          PlayerState.Big -> {
            BackHandler(enabled = true) { viewModel.onEvent(PlayerEvent.Small) }
            BigPlayerContent(
              player = player,
              id = uiState.value.id,
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.progress,
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
            )
          }
          is PlayerState.Hidden,
          PlayerState.TempHidden -> {}
        }
      }
    }
  }
}

@UnstableApi
@Composable
fun PlayPauseButton(player: Player, id: String, size: Int) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      val playPauseState = rememberPlayPauseButtonState(player)
      val icon = if (playPauseState.showPlay) Icons.Default.PlayArrow else Icons.Default.Pause
      val contentDescription = if (playPauseState.showPlay) "Play" else "Pause"
      MyIconButton(
        modifier = Modifier.size(size.dp).mySharedBound(Animations.Companion.Player.playKey(id)),
        icon = icon,
        contentDescription = contentDescription,
        onClick = playPauseState::onClick,
        enabled = playPauseState.isEnabled,
      )
    }
  }
}

@UnstableApi
@Composable
fun SeekBackButton(player: Player, id: String, size: Int = 48) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      val state = rememberSeekBackButtonState(player)
      MyIconButton(
        modifier =
          Modifier.mySharedBound(Animations.Companion.Player.seekBackKey(id)),
        icon = Icons.Default.Replay10,
        contentDescription = "Seek Back",
        size = size,
        onClick = state::onClick,
        enabled = state.isEnabled,
      )
    }
  }
}

@UnstableApi
@Composable
fun SeekForwardButton(player: Player, id: String, size: Int = 48) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      val state = rememberSeekForwardButtonState(player)
      MyIconButton(
        modifier =
          Modifier.size(size.dp).mySharedBound(Animations.Companion.Player.seekForwardKey(id)),
        icon = Icons.Default.Forward10,
        contentDescription = "Seek Forward",
        size = size,
        onClick = state::onClick,
        enabled = state.isEnabled,
      )
    }
  }
}
