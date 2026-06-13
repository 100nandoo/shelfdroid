package dev.halim.shelfdroid.widget.playback

import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackWidgetSleepTimerTextTest {
  @Test
  fun `zero duration has no active timer label`() {
    assertEquals(null, 0.seconds.toPlaybackWidgetSleepTimerText())
  }

  @Test
  fun `sub minute duration uses seconds`() {
    assertEquals("59s", 59.seconds.toPlaybackWidgetSleepTimerText())
  }

  @Test
  fun `minute duration rounds up to minutes`() {
    assertEquals("2m", 61.seconds.toPlaybackWidgetSleepTimerText())
  }
}
