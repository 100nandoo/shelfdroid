package dev.halim.shelfdroid.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackWidgetSizingTest {
  @Test
  fun responsiveSizesContainOnlyThreeByTwoAndFourByTwoLayouts() {
    val sizeMode = PlaybackWidget().sizeMode

    assertEquals(
      setOf(SmallPlaybackWidgetSize, LargePlaybackWidgetSize),
      sizeMode.sizes,
    )
  }
}
