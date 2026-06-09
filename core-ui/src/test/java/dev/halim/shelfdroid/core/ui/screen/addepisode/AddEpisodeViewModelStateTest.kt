package dev.halim.shelfdroid.core.ui.screen.addepisode

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEpisodeViewModelStateTest {
  @Test
  fun withFilterPreferences_preservesTransientFilterCriteria() {
    val merged =
      AddEpisodeUiState(
          filterState =
            AddEpisodeFilterState(
              titleQuery = "android",
              publishedStartDateMillis = 1L,
              publishedEndDateMillis = 2L,
              hideDownloaded = false,
            )
        )
        .withFilterPreferences(hideDownloaded = true, downloadState = GenericState.Loading)

    assertEquals("android", merged.filterState.titleQuery)
    assertEquals(1L, merged.filterState.publishedStartDateMillis)
    assertEquals(2L, merged.filterState.publishedEndDateMillis)
    assertTrue(merged.filterState.hideDownloaded)
    assertEquals(GenericState.Loading, merged.downloadEpisodeState)
  }
}
