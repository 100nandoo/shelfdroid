package dev.halim.shelfdroid.core.ui.screen.podcast

import org.junit.Assert.assertEquals
import org.junit.Test

class PodcastInteractionStateTest {

  @Test
  fun openEpisodeActions_ignoresRequestsWhenSelectionModeIsActive() {
    val state =
      PodcastInteractionState(
        isSelectionMode = true,
        selectedEpisodeIds = setOf("ep-1"),
      )

    val updated = state.openEpisodeActions(episodeId = "ep-2", canEdit = true, canDelete = false)

    assertEquals(state, updated)
  }

  @Test
  fun openEpisodeActions_requiresAtLeastOneManagementPermission() {
    val state = PodcastInteractionState()

    val updated = state.openEpisodeActions(episodeId = "ep-1", canEdit = false, canDelete = false)

    assertEquals(state, updated)
  }

  @Test
  fun startDeleteSelectionFromActions_preselectsEpisodeAndClearsSheet() {
    val state = PodcastInteractionState(actionSheetEpisodeId = "ep-1")

    val updated =
      state.startDeleteSelectionFromActions(
        episodeId = "ep-1",
        autoSelectedIds = setOf("ep-finished"),
      )

    assertEquals(true, updated.isSelectionMode)
    assertEquals(setOf("ep-1", "ep-finished"), updated.selectedEpisodeIds)
    assertEquals(null, updated.actionSheetEpisodeId)
  }
}
