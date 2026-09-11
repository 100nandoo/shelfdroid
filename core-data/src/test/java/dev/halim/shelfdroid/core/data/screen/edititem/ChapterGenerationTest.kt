package dev.halim.shelfdroid.core.data.screen.edititem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterGenerationTest {

  @Test
  fun generateChaptersFromTracks_skipsExcludedTracksAndUsesFilenameWithoutExtension() {
    val chapters =
      generateChaptersFromTracks(
          listOf(
            ChapterSourceTrack(filename = "01 - Intro.mp3", duration = 12.5),
            ChapterSourceTrack(filename = "excluded.mp3", duration = 20.0, excluded = true),
            ChapterSourceTrack(filename = "02 - Chapter.m4b", duration = 30.25),
          )
        )
        .getOrThrow()

    assertEquals(
      listOf(
        ChapterRow(id = 0, title = "01 - Intro", start = 0.0, end = 12.5),
        ChapterRow(id = 1, title = "02 - Chapter", start = 12.5, end = 42.75),
      ),
      chapters,
    )
  }

  @Test
  fun generateChaptersFromTracks_rejectsInvalidTrackData() {
    val result =
      generateChaptersFromTracks(
        listOf(ChapterSourceTrack(filename = "01.mp3", duration = 0.0))
      )

    assertTrue(result.isFailure)
  }
}
