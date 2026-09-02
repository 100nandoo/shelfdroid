package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import dev.halim.shelfdroid.core.data.task.TaskAction
import dev.halim.shelfdroid.core.data.task.TaskStatus
import dev.halim.shelfdroid.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskPresentationTest {

  @Test
  fun knownActionsMapEachStatusToTheirOperationPresentation() {
    assertEquals(
      R.string.library_scan_completed,
      taskPresentation(TaskAction.LibraryScan, TaskStatus.COMPLETED).statusLabel,
    )
    assertEquals(
      R.string.library_match_failed,
      taskPresentation(TaskAction.BookMatching, TaskStatus.FAILED).statusLabel,
    )
  }

  @Test
  fun unknownActionUsesGenericStatusAndDoesNotInventResultLabels() {
    val presentation =
      taskPresentation(
        TaskAction.Unknown("future-server-task"),
        TaskStatus.COMPLETED,
      )

    assertEquals(TaskPresentationKind.UNKNOWN, presentation.kind)
    assertEquals(R.string.library_task_completed, presentation.statusLabel)
    assertEquals(null, presentation.countsLabel)
    assertEquals(null, presentation.elapsedLabel)
  }
}
