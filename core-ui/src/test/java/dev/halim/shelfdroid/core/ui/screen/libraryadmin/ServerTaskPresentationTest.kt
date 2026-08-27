package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import dev.halim.shelfdroid.core.data.task.ServerTaskAction
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
import dev.halim.shelfdroid.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ServerTaskPresentationTest {

  @Test
  fun knownActionsMapEachStatusToTheirOperationPresentation() {
    assertEquals(
      R.string.library_scan_completed,
      serverTaskPresentation(ServerTaskAction.LibraryScan, ServerTaskStatus.COMPLETED).statusLabel,
    )
    assertEquals(
      R.string.library_match_failed,
      serverTaskPresentation(ServerTaskAction.BookMatching, ServerTaskStatus.FAILED).statusLabel,
    )
  }

  @Test
  fun unknownActionUsesGenericStatusAndDoesNotInventResultLabels() {
    val presentation =
      serverTaskPresentation(
        ServerTaskAction.Unknown("future-server-task"),
        ServerTaskStatus.COMPLETED,
      )

    assertEquals(ServerTaskPresentationKind.UNKNOWN, presentation.kind)
    assertEquals(R.string.library_task_completed, presentation.statusLabel)
    assertEquals(null, presentation.countsLabel)
    assertEquals(null, presentation.elapsedLabel)
  }
}
