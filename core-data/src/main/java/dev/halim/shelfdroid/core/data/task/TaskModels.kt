package dev.halim.shelfdroid.core.data.task

/** Connection state used by the application-scoped Server task repository. */
enum class TaskConnectionState {
  UNKNOWN,
  CONNECTED,
  DISCONNECTED,
}

enum class TaskStatus {
  ACTIVE,
  COMPLETED,
  FAILED,
  CANCELLED,
}

enum class TaskSyncState {
  NOT_STARTED,
  SYNCHRONIZING,
  SUCCEEDED,
  FAILED,
}

sealed interface TaskError {
  data class SafeMessage(val message: String) : TaskError

  data object Generic : TaskError
}

data class TaskResult(
  val added: Int? = null,
  val updated: Int? = null,
  val missing: Int? = null,
  val elapsedMillis: Long? = null,
)

/** A stable app model for every server task, independent of the operation that created it. */
data class Task(
  val id: String,
  val action: TaskAction,
  val libraryId: String?,
  val title: String? = null,
  val status: TaskStatus,
  val startedAt: Long? = null,
  val finishedAt: Long? = null,
  val result: TaskResult? = null,
  val error: TaskError? = null,
  val syncState: TaskSyncState = TaskSyncState.NOT_STARTED,
  val syncError: TaskError? = null,
)

data class TaskRepositoryState(
  val connectionState: TaskConnectionState = TaskConnectionState.UNKNOWN,
  val snapshotKnown: Boolean = false,
  val tasks: List<Task> = emptyList(),
)

data class TaskNotification(
  val taskId: String,
  val status: TaskStatus,
  val action: TaskAction,
)
