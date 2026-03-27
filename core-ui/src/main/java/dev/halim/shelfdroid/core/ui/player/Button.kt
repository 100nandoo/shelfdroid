package dev.halim.shelfdroid.core.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.MultipleButtonState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyFilledTonalIconButton
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun PlayPauseButton(
  onClick: () -> Unit,
  multipleButtonState: MultipleButtonState,
  id: String,
  size: Int = 48,
) {
  val icon =
    if (multipleButtonState.showPlay) painterResource(R.drawable.play_arrow)
    else painterResource(R.drawable.pause)
  val contentDescription =
    if (multipleButtonState.showPlay) stringResource(R.string.play)
    else stringResource(R.string.pause)
  MyFilledTonalIconButton(
    modifier = Modifier.mySharedBound(Animations.Companion.Player.playKey(id)),
    enabled = multipleButtonState.playPauseEnabled,
    onClick = { onClick() },
    contentDescription = contentDescription,
    painter = icon,
    size = size,
  )
}

@Composable
fun SeekBackButton(onClick: () -> Unit, state: MultipleButtonState, id: String, size: Int = 48) {
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
fun SeekForwardButton(onClick: () -> Unit, state: MultipleButtonState, id: String, size: Int = 48) {
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
          PlayPauseButton({}, MultipleButtonState(playPauseEnabled = true), "1", 72)
          PlayPauseButton(
            {},
            MultipleButtonState(playPauseEnabled = true, showPlay = false),
            "2",
            72,
          )
          PlayPauseButton(
            {},
            MultipleButtonState(playPauseEnabled = false, showPlay = true),
            "3",
            72,
          )
          PlayPauseButton(
            {},
            MultipleButtonState(playPauseEnabled = false, showPlay = false),
            "4",
            72,
          )
        }
        Row {
          PlayPauseButton({}, MultipleButtonState(playPauseEnabled = true), "5")
          PlayPauseButton({}, MultipleButtonState(playPauseEnabled = true, showPlay = false), "6")
          PlayPauseButton({}, MultipleButtonState(playPauseEnabled = false, showPlay = true), "7")
          PlayPauseButton({}, MultipleButtonState(playPauseEnabled = false, showPlay = false), "8")
        }
      }
    },
  )
}
