package dev.halim.shelfdroid.media.mediaitem

import dev.halim.shelfdroid.core.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMediaMetadataTest {
  @Test
  fun metadataIdentifiesLibraryItemAndCurrentPlayableUnitSeparately() {
    val titles =
      playbackMetadataTitles(
        PlayerUiState(
          title = "Chapter 1",
          libraryItemTitle = "The Left Hand of Darkness",
          author = "Ursula K. Le Guin",
          cover = "https://example.test/cover",
        )
      )

    assertEquals("Chapter 1", titles.playableTitle)
    assertEquals("The Left Hand of Darkness", titles.libraryItemTitle)
  }
}
