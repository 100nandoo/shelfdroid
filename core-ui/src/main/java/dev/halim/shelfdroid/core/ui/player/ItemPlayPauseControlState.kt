package dev.halim.shelfdroid.core.ui.player

import dev.halim.shelfdroid.core.PlayPauseControlState

internal fun PlayPauseControlState.forItemAction(isCurrentItem: Boolean): PlayPauseControlState {
  if (!isCurrentItem) return PlayPauseControlState(enabled = true)
  return copy(enabled = true)
}
