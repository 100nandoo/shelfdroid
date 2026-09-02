package dev.halim.shelfdroid.core.data.task

import kotlinx.coroutines.flow.StateFlow

interface TaskRepositoryContract {
  val state: StateFlow<TaskRepositoryState>

  /** The oldest unacknowledged terminal notification, retained across screen recreation. */
  val notifications: StateFlow<TaskNotification?>

  suspend fun refresh(): Result<Unit>

  /** The HTTP response means accepted/started; task completion is never inferred here. */
  suspend fun startLibraryScan(libraryId: String): Result<Unit>

  /** The match-all HTTP response means accepted/started; completion is socket/task state. */
  suspend fun startLibraryMatch(libraryId: String): Result<Unit>

  suspend fun retrySynchronization(taskId: String): Result<Unit>

  fun acknowledgeNotification(taskId: String)
}
