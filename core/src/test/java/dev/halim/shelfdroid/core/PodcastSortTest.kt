package dev.halim.shelfdroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastSortTest {

  @Test
  fun fromLabel_parsesProgress() {
    assertEquals(PodcastSort.Progress, PodcastSort.fromLabel(LABEL_PROGRESS))
  }
}
