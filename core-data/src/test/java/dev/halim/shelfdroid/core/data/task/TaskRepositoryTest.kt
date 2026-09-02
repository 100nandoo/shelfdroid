package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.response.ServerTask as NetworkServerTask
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskRepositoryTest {

  @Test
  fun actionMapping_recognizesSupportedActionsAndRetainsUnknownWireValue() {
    assertEquals(TaskAction.LibraryScan, TaskAction.fromRaw("library-scan"))
    assertEquals(TaskAction.BookMatching, TaskAction.fromRaw("library-match-all"))
    assertEquals(
      TaskAction.Unknown("future-server-task"),
      TaskAction.fromRaw("future-server-task"),
    )
    assertEquals(
      "future-server-task",
      (TaskAction.fromRaw("future-server-task") as TaskAction.Unknown).rawValue,
    )
  }

  @Test
  fun scanTask_mapsLibraryAndResultCountsFromFinishedPayload() {
    val task =
      NetworkServerTask(
        id = "task-1",
        action = "library-scan",
        data =
          buildJsonObject {
            put("libraryId", "books")
            putJsonObject("scanResults") {
              put("added", 2)
              put("updated", 3)
              put("missing", 1)
              put("elapsed", 4_500)
            }
          },
        isFinished = true,
        startedAt = 10,
        finishedAt = 4_510,
      )

    val mapped = task.toDomainTask()

    assertEquals("books", mapped.libraryId)
    assertEquals(TaskAction.LibraryScan, mapped.action)
    assertEquals(TaskStatus.COMPLETED, mapped.status)
    assertEquals(TaskResult(2, 3, 1, 4_500), mapped.result)
  }

  @Test
  fun failedAndCancelledTasks_areDistinctAndInternalErrorsAreGeneric() {
    val failed =
      NetworkServerTask(
        id = "failed",
        action = "library-scan",
        isFinished = true,
        isFailed = true,
        error = "IllegalStateException: stack at internal/file.js:12",
      )
    val cancelled =
      NetworkServerTask(
        id = "cancelled",
        action = "library-scan",
        isFinished = true,
        descriptionKey = "MessageTaskCanceledByUser",
      )

    val failedMapped = failed.toDomainTask()
    val cancelledMapped = cancelled.toDomainTask()

    assertEquals(TaskStatus.FAILED, failedMapped.status)
    assertEquals(TaskError.Generic, failedMapped.error)
    assertEquals(TaskStatus.CANCELLED, cancelledMapped.status)
    assertNull(cancelledMapped.error)
  }

  @Test
  fun safeServerErrorText_isPreservedAsDisplayableData() {
    val task =
      NetworkServerTask(
        id = "failed",
        action = "library-scan",
        isFinished = true,
        isFailed = true,
        error = "The selected library folder is unavailable.",
      )

    assertEquals(
      TaskError.SafeMessage("The selected library folder is unavailable."),
      task.toDomainTask().error,
    )
  }

  @Test
  fun activeTask_remainsActiveUntilServerCompletion() {
    val task = NetworkServerTask(id = "active", action = "library-scan", isFinished = false)

    val mapped = task.toDomainTask()

    assertEquals(TaskStatus.ACTIVE, mapped.status)
    assertTrue(mapped.syncState == TaskSyncState.NOT_STARTED)
  }

  @Test
  fun unknownAction_isRetainedWhenMappingNetworkTask() {
    val mapped =
      NetworkServerTask(
          id = "future",
          action = "future-server-task",
          isFinished = true,
        )
        .toDomainTask()

    assertEquals(TaskAction.Unknown("future-server-task"), mapped.action)
    assertEquals("future-server-task", mapped.action.rawValue)
  }
}
