package dev.halim.shelfdroid.core.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun rememberSeekBackButtonState(player: Player): SeekBackButtonState {
  val buttonState = remember(player) { SeekBackButtonState(player) }
  LaunchedEffect(player) { buttonState.observe() }
  return buttonState
}

class SeekBackButtonState(private val player: Player) {
  var isEnabled by mutableStateOf(player.isCommandAvailable(Player.COMMAND_SEEK_BACK))
    private set

  fun onClick() {
    player.seekBack()
  }

  suspend fun observe(): Nothing =
    player.listen { events ->
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        isEnabled = isCommandAvailable(Player.COMMAND_SEEK_BACK)
      }
    }
}

@UnstableApi
@Composable
fun rememberSeekForwardButtonState(player: Player): SeekForwardButtonState {
  val buttonState = remember(player) { SeekForwardButtonState(player) }
  LaunchedEffect(player) { buttonState.observe() }
  return buttonState
}

class SeekForwardButtonState(private val player: Player) {
  var isEnabled by mutableStateOf(player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD))
    private set

  fun onClick() {
    player.seekForward()
  }

  suspend fun observe(): Nothing =
    player.listen { events ->
      if (events.contains(Player.EVENT_AVAILABLE_COMMANDS_CHANGED)) {
        isEnabled = isCommandAvailable(Player.COMMAND_SEEK_FORWARD)
      }
    }
}
