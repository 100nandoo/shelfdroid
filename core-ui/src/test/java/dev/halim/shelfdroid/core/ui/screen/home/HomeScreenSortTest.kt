package dev.halim.shelfdroid.core.ui.screen.home

import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenSortTest {

  @Test
  fun podcastFilterAndSort_whenSortByProgressDesc_ordersByRecencyAndUsesTitleTieBreak() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(title = "Zulu", progressLastUpdate = 0L),
            podcast(title = "Charlie", progressLastUpdate = 200L),
            podcast(title = "Alpha", progressLastUpdate = 0L),
            podcast(title = "Bravo", progressLastUpdate = 200L),
          ),
        displayPrefs =
          DisplayPrefs(
            filter = Filter.All,
            podcastSort = PodcastSort.Progress,
            podcastSortOrder = SortOrder.Desc,
          ),
      )

    assertEquals(listOf("Bravo", "Charlie", "Alpha", "Zulu"), sorted.map(PodcastUiState::title))
  }

  @Test
  fun podcastFilterAndSort_whenSortByProgressAsc_ordersByRecencyAndUsesTitleTieBreak() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(title = "Zulu", progressLastUpdate = 0L),
            podcast(title = "Charlie", progressLastUpdate = 200L),
            podcast(title = "Alpha", progressLastUpdate = 0L),
            podcast(title = "Bravo", progressLastUpdate = 200L),
          ),
        displayPrefs =
          DisplayPrefs(
            filter = Filter.All,
            podcastSort = PodcastSort.Progress,
            podcastSortOrder = SortOrder.Asc,
          ),
      )

    assertEquals(listOf("Alpha", "Zulu", "Bravo", "Charlie"), sorted.map(PodcastUiState::title))
  }

  private fun podcast(title: String, progressLastUpdate: Long): PodcastUiState {
    return PodcastUiState(
      id = title.lowercase(),
      title = title,
      progressLastUpdate = progressLastUpdate,
    )
  }
}
