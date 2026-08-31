package dev.halim.shelfdroid.media.sessionreset

import dev.halim.shelfdroid.media.presentation.PlaybackPresentationNotifier
import dev.halim.shelfdroid.media.presentation.PlaybackPresentationObserver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCurrentPlaybackCleanupTest {

  @Test
  fun clearCurrentPlayback_clearsStateThenControllerThenPresentation() = runTest {
    val events = mutableListOf<String>()
    val notifier =
      PlaybackPresentationNotifier(
        setOf(PlaybackPresentationObserver { events += "presentation" })
      )
    val cleanup =
      MediaCurrentPlaybackCleanup(
        clearPlayerStore = { events += "player store" },
        clearMediaController = { events += "media controller" },
        notifyPresentationChanged = notifier::notifyPresentationChanged,
      )

    cleanup.clearCurrentPlayback()

    assertEquals(listOf("player store", "media controller", "presentation"), events)
  }
}
