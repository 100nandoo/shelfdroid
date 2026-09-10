package dev.halim.shelfdroid.core

import dev.halim.shelfdroid.core.prefs.LABEL_PROGRESS
import dev.halim.shelfdroid.core.prefs.PodcastSort
import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastSortTest {

  @Test
  fun fromLabel_parsesProgress() {
    assertEquals(PodcastSort.Progress, PodcastSort.fromLabel(LABEL_PROGRESS))
  }
}
