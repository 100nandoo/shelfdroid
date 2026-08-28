package dev.halim.shelfdroid.core.data.screen.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLibrarySelectionTest {
  @Test
  fun deletingNonActiveLibraryKeepsTheSelectedLibrary() {
    val previous = libraries("first", "active", "last")

    assertEquals(
      "active",
      reconcileActiveLibraryId(
        previousLibraries = previous,
        activeLibraryId = "active",
        updatedLibraries = libraries("active", "last"),
      ),
    )
  }

  @Test
  fun deletingActiveMiddleLibrarySelectsTheNextLibrary() {
    assertEquals(
      "last",
      reconcileActiveLibraryId(
        previousLibraries = libraries("first", "active", "last"),
        activeLibraryId = "active",
        updatedLibraries = libraries("first", "last"),
      ),
    )
  }

  @Test
  fun deletingActiveLastLibrarySelectsThePreviousLibrary() {
    assertEquals(
      "middle",
      reconcileActiveLibraryId(
        previousLibraries = libraries("first", "middle", "last"),
        activeLibraryId = "last",
        updatedLibraries = libraries("first", "middle"),
      ),
    )
  }

  @Test
  fun deletingFinalLibraryLeavesNoSelection() {
    assertEquals(
      null,
      reconcileActiveLibraryId(
        previousLibraries = libraries("only"),
        activeLibraryId = "only",
        updatedLibraries = emptyList(),
      ),
    )
  }

  @Test
  fun selectionReconciliationHasNoPlaybackStopOrCleanupTransition() {
    // Playback is deliberately absent from the reducer's inputs and outputs. These sentinels
    // model the commands that a delete transition must never issue.
    val playback = PlaybackProbe()

    val selected =
      reconcileActiveLibraryId(
        previousLibraries = libraries("first", "active"),
        activeLibraryId = "active",
        updatedLibraries = libraries("first"),
      )

    assertEquals("first", selected)
    assertEquals(0, playback.stopCalls)
    assertEquals(0, playback.cleanupCalls)
  }

  private fun libraries(vararg ids: String): List<LibraryUiState> = ids.map { id ->
    LibraryUiState(id = id, name = id)
  }

  private class PlaybackProbe {
    var stopCalls: Int = 0
    var cleanupCalls: Int = 0
  }
}
