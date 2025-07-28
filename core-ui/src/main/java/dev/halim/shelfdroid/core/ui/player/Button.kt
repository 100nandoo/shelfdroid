@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.MultipleButtonState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound

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
      val contentDescription =
        if (multipleButtonState.showPlay) stringResource(R.string.play)
        else stringResource(R.string.pause)
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
        icon = Icons.Default.FastRewind,
        size = size,
        contentDescription = stringResource(R.string.seek_back),
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
        icon = Icons.Default.FastForward,
        size = size,
        contentDescription = stringResource(R.string.seek_forward),
        onClick = { onClick() },
        enabled = state.seekForwardEnabled,
      )
    }
  }
}
