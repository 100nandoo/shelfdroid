package dev.halim.shelfdroid.media.sessionreset

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaCurrentPlaybackCleanupTest {

  @Test
  fun clearCurrentPlayback_clearsPlayerStoreBeforeMediaController() {
    val events = mutableListOf<String>()
    val cleanup =
      MediaCurrentPlaybackCleanup(
        clearPlayerStore = { events += "player store" },
        clearMediaController = { events += "media controller" },
      )

    cleanup.clearCurrentPlayback()

    assertEquals(listOf("player store", "media controller"), events)
  }
}
