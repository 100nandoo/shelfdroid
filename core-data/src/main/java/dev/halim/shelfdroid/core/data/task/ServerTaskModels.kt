package dev.halim.shelfdroid.core.data.task

/** Connection state used by the application-scoped Server task repository. */
enum class ServerTaskConnectionState {
  UNKNOWN,
  CONNECTED,
  DISCONNECTED,
}

enum class ServerTaskStatus {
  ACTIVE,
  COMPLETED,
  FAILED,
  CANCELLED,
}

enum class ServerTaskSyncState {
  NOT_STARTED,
  SYNCHRONIZING,
  SUCCEEDED,
  FAILED,
}

sealed interface ServerTaskError {
  data class SafeMessage(val message: String) : ServerTaskError

  data object Generic : ServerTaskError
}

data class ServerTaskResult(
  val added: Int? = null,
  val updated: Int? = null,
  val missing: Int? = null,
  val elapsedMillis: Long? = null,
)

/** A stable app model for every server task, independent of the operation that created it. */
data class ServerTask(
  val id: String,
  val action: ServerTaskAction,
  val libraryId: String?,
  val title: String? = null,
  val status: ServerTaskStatus,
  val startedAt: Long? = null,
  val finishedAt: Long? = null,
  val result: ServerTaskResult? = null,
  val error: ServerTaskError? = null,
  val syncState: ServerTaskSyncState = ServerTaskSyncState.NOT_STARTED,
  val syncError: ServerTaskError? = null,
)

data class ServerTaskRepositoryState(
  val connectionState: ServerTaskConnectionState = ServerTaskConnectionState.UNKNOWN,
  val snapshotKnown: Boolean = false,
  val tasks: List<ServerTask> = emptyList(),
)

data class ServerTaskNotification(
  val taskId: String,
  val status: ServerTaskStatus,
  val action: ServerTaskAction,
)
