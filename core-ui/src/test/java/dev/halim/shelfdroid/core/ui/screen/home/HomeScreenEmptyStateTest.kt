package dev.halim.shelfdroid.core.ui.screen.home

import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenEmptyStateTest {

  @Test
  fun shouldShowDownloadedEmptyState_whenDownloadedFilterHasNoResults_returnsTrue() {
    val shouldShow =
      shouldShowDownloadedEmptyState(
        displayPrefs = DisplayPrefs(filter = Filter.Downloaded),
        processedBooks = emptyList(),
        processedPodcasts = emptyList(),
      )

    assertTrue(shouldShow)
  }

  @Test
  fun shouldShowDownloadedEmptyState_whenDownloadedFilterHasResults_returnsFalse() {
    val shouldShow =
      shouldShowDownloadedEmptyState(
        displayPrefs = DisplayPrefs(filter = Filter.Downloaded),
        processedBooks = listOf(BookUiState(id = "book")),
        processedPodcasts = listOf(PodcastUiState(id = "podcast")),
      )

    assertFalse(shouldShow)
  }

  @Test
  fun shouldShowDownloadedEmptyState_whenAllFilterHasNoResults_returnsFalse() {
    val shouldShow =
      shouldShowDownloadedEmptyState(
        displayPrefs = DisplayPrefs(filter = Filter.All),
        processedBooks = emptyList(),
        processedPodcasts = emptyList(),
      )

    assertFalse(shouldShow)
  }
}
