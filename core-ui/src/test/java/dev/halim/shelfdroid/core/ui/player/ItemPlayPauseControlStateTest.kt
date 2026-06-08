package dev.halim.shelfdroid.core.ui.player

import dev.halim.shelfdroid.core.PlayPauseControlState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemPlayPauseControlStateTest {
  @Test
  fun current_item_keeps_loading_pause_semantics() {
    val resolved =
      PlayPauseControlState(
          enabled = false,
          isPlaying = false,
          showPlayIcon = false,
          showLoadingIndicator = true,
        )
        .forItemAction(isCurrentItem = true)

    assertTrue(resolved.enabled)
    assertFalse(resolved.showPlayIcon)
    assertTrue(resolved.showLoadingIndicator)
    assertFalse(resolved.isPlaying)
  }

  @Test
  fun non_current_item_stays_inactive_while_another_item_loads() {
    val resolved =
      PlayPauseControlState(
          enabled = false,
          isPlaying = true,
          showPlayIcon = false,
          showLoadingIndicator = true,
        )
        .forItemAction(isCurrentItem = false)

    assertTrue(resolved.enabled)
    assertTrue(resolved.showPlayIcon)
    assertFalse(resolved.showLoadingIndicator)
    assertFalse(resolved.isPlaying)
  }

  @Test
  fun current_item_keeps_pause_visual_when_playing() {
    val resolved =
      PlayPauseControlState(
          enabled = false,
          isPlaying = true,
          showPlayIcon = false,
          showLoadingIndicator = false,
        )
        .forItemAction(isCurrentItem = true)

    assertTrue(resolved.enabled)
    assertTrue(resolved.isPlaying)
    assertFalse(resolved.showPlayIcon)
    assertFalse(resolved.showLoadingIndicator)
  }
}
