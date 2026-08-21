package dev.halim.shelfdroid.core.data.screen.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDownloadStatusTest {

  @Test
  fun isBookFullyDownloaded_requiresEveryTrack() {
    assertTrue(isBookFullyDownloaded(listOf("one.mp3", "two.mp3"), setOf("one.mp3", "two.mp3")))
    assertFalse(isBookFullyDownloaded(listOf("one.mp3", "two.mp3"), setOf("one.mp3")))
    assertFalse(isBookFullyDownloaded(emptyList(), setOf("one.mp3")))
  }

  @Test
  fun podcastDownloadCounts_reportsDownloadedAndUnfinishedEpisodes() {
    val counts =
      podcastDownloadCounts(
        downloadedEpisodeIds = setOf("finished", "unfinished", "extra"),
        finishedEpisodeIds = setOf("finished"),
      )

    assertEquals(3, counts.downloadedCount)
    assertEquals(2, counts.unfinishedAndDownloadCount)
  }
}
