package dev.halim.shelfdroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsDefaultsTest {

  @Test
  fun displayPrefs_defaultsPodcastLibraryToProgressDescending() {
    val prefs = DisplayPrefs()

    assertEquals(PodcastSort.Progress, prefs.podcastSort)
    assertEquals(SortOrder.Desc, prefs.podcastSortOrder)
  }

  @Test
  fun playerPrefs_defaultsChapterTimeDisplayToShortDuration() {
    val prefs = PlayerPrefs()

    assertEquals(ChapterTimeDisplay.DurationShort, prefs.chapterTimeDisplay)
  }
}
