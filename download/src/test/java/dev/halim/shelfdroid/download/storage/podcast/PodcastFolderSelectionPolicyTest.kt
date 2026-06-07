package dev.halim.shelfdroid.download.storage.podcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastFolderSelectionPolicyTest {
  private val policy = PodcastFolderSelectionPolicy()

  @Test
  fun `keeps exact relative path when current folder already has the episode`() {
    val resolved =
      policy.resolveRelativePath(
        exactRelativePath = "Download/ShelfDroid/podcasts/The Daily/",
        filename = "2026-06-06.mp3",
        matches = listOf("Download/ShelfDroid/podcasts/The Daily/"),
      )

    assertEquals("Download/ShelfDroid/podcasts/The Daily/", resolved)
  }

  @Test
  fun `reuses previous folder when metadata changed and the match is unambiguous`() {
    val resolved =
      policy.resolveRelativePath(
        exactRelativePath = "Download/ShelfDroid/podcasts/The Daily Rebrand/",
        filename = "2026-06-06.mp3",
        matches = listOf("Download/ShelfDroid/podcasts/The Daily/"),
      )

    assertEquals("Download/ShelfDroid/podcasts/The Daily/", resolved)
  }

  @Test
  fun `refuses to recover when podcast matches are ambiguous`() {
    val resolved =
      policy.resolveRelativePath(
        exactRelativePath = "Download/ShelfDroid/podcasts/The Daily Rebrand/",
        filename = "2026-06-06.mp3",
        matches =
          listOf(
            "Download/ShelfDroid/podcasts/The Daily/",
            "Download/ShelfDroid/podcasts/Today Explained/",
          ),
      )

    assertNull(resolved)
  }
}
