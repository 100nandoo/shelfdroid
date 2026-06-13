package dev.halim.shelfdroid.widget.playback

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWidgetTransportDispatcherTest {
  private val dispatcher = PlaybackWidgetTransportDispatcher()

  @Test
  fun `play pause pauses active playback`() = runTest {
    val controller = FakePlaybackWidgetController(isPlaying = true, isPlayPauseEnabled = true)

    val result = dispatcher.dispatch(PlaybackTransportAction.PlayPause) { controller }

    assertEquals(PlaybackWidgetTransportResult.Dispatched, result)
    assertEquals(0, controller.playCalls)
    assertEquals(1, controller.pauseCalls)
    assertTrue(controller.released)
  }

  @Test
  fun `seek back returns not available when command is disabled`() = runTest {
    val controller = FakePlaybackWidgetController(isSeekBackEnabled = false)

    val result = dispatcher.dispatch(PlaybackTransportAction.SeekBack) { controller }

    assertEquals(PlaybackWidgetTransportResult.NotAvailable, result)
    assertEquals(0, controller.seekBackCalls)
    assertTrue(controller.released)
  }

  @Test
  fun `connection failure falls back to opening the app`() = runTest {
    val result = dispatcher.dispatch(PlaybackTransportAction.PlayPause) { null }

    assertEquals(PlaybackWidgetTransportResult.OpenAppFallback, result)
  }

  private class FakePlaybackWidgetController(
    override val isPlaying: Boolean = false,
    override val isPlayPauseEnabled: Boolean = false,
    override val isSeekBackEnabled: Boolean = false,
    override val isSeekForwardEnabled: Boolean = false,
  ) : PlaybackWidgetController {
    var playCalls = 0
    var pauseCalls = 0
    var seekBackCalls = 0
    var seekForwardCalls = 0
    var released = false

    override fun play() {
      playCalls += 1
    }

    override fun pause() {
      pauseCalls += 1
    }

    override fun seekBack() {
      seekBackCalls += 1
    }

    override fun seekForward() {
      seekForwardCalls += 1
    }

    override fun release() {
      released = true
    }
  }
}
