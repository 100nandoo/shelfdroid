package dev.halim.shelfdroid.core.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.SeekControlsState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun PlayPauseButton(
  onClick: () -> Unit,
  playPause: PlayPauseControlState,
  id: String,
  size: Int = 48,
) {
  val icon =
    if (playPause.showPlayIcon) painterResource(R.drawable.play_arrow)
    else painterResource(R.drawable.pause)
  val contentDescription =
    if (playPause.showPlayIcon) stringResource(R.string.play) else stringResource(R.string.pause)
  val contentSize = (size - 2 * (size / 6)).dp
  FilledTonalIconButton(
    modifier = Modifier.size(size.dp).mySharedBound(Animations.Companion.Player.playKey(id)),
    enabled = playPause.enabled,
    onClick = onClick,
  ) {
    Box(
      modifier = Modifier.size(contentSize),
      contentAlignment = Alignment.Center,
    ) {
      if (playPause.showLoadingIndicator) {
        CircularProgressIndicator(
          modifier = Modifier.size(contentSize).padding(6.dp),
          color = LocalContentColor.current,
        )
      } else {
        Icon(
          modifier = Modifier.size(contentSize),
          painter = icon,
          contentDescription = contentDescription,
        )
      }
    }
  }
}

@Composable
fun SeekBackButton(onClick: () -> Unit, state: SeekControlsState, id: String, size: Int = 48) {
  MyIconButton(
    modifier = Modifier.mySharedBound(Animations.Companion.Player.seekBackKey(id)),
    painter = painterResource(R.drawable.fast_rewind),
    size = size,
    contentDescription = stringResource(R.string.seek_back),
    onClick = { onClick() },
    enabled = state.seekBackEnabled,
  )
}

@Composable
fun SeekForwardButton(onClick: () -> Unit, state: SeekControlsState, id: String, size: Int = 48) {
  MyIconButton(
    modifier = Modifier.mySharedBound(Animations.Companion.Player.seekForwardKey(id)),
    painter = painterResource(R.drawable.fast_forward),
    size = size,
    contentDescription = stringResource(R.string.seek_forward),
    onClick = { onClick() },
    enabled = state.seekForwardEnabled,
  )
}

@ShelfDroidPreview
@Composable
fun PlayPauseButtonLargePreview() {
  AnimatedPreviewWrapper(
    dynamicColor = false,
    content = {
      Column {
        Row {
          PlayPauseButton({}, PlayPauseControlState(enabled = true), "1", 72)
          PlayPauseButton(
            {},
            PlayPauseControlState(enabled = true, showPlayIcon = false),
            "2",
            72,
          )
          PlayPauseButton(
            {},
            PlayPauseControlState(enabled = false, showPlayIcon = true),
            "3",
            72,
          )
          PlayPauseButton(
            {},
            PlayPauseControlState(
              enabled = true,
              showPlayIcon = false,
              showLoadingIndicator = true,
            ),
            "4",
            72,
          )
        }
        Row {
          PlayPauseButton({}, PlayPauseControlState(enabled = true), "5")
          PlayPauseButton({}, PlayPauseControlState(enabled = true, showPlayIcon = false), "6")
          PlayPauseButton({}, PlayPauseControlState(enabled = false, showPlayIcon = true), "7")
          PlayPauseButton(
            {},
            PlayPauseControlState(
              enabled = true,
              showPlayIcon = false,
              showLoadingIndicator = true,
            ),
            "8",
          )
        }
      }
    },
  )
}
