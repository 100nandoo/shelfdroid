package dev.halim.shelfdroid.core.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDragTransitionTest {
  @Test
  fun drag_reaches_halfway_threshold() {
    assertTrue(shouldCollapsePlayerDrag(progress = PlayerCollapseThreshold, velocity = 0f))
  }

  @Test
  fun fast_downward_fling_commits_before_threshold() {
    assertTrue(shouldCollapsePlayerDrag(progress = 0.1f, velocity = PlayerCollapseFlingVelocity))
  }

  @Test
  fun short_slow_drag_restores_expanded_player() {
    assertFalse(shouldCollapsePlayerDrag(progress = 0.49f, velocity = 500f))
  }
}
