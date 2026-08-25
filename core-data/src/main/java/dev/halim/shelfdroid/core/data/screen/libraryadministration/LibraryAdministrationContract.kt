package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val EMPTY_TASK_STATE = MutableStateFlow(ServerTaskRepositoryState())
private val EMPTY_TASK_NOTIFICATIONS = MutableStateFlow<ServerTaskNotification?>(null)

interface LibraryAdministrationContract {
  suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>>

  /** Task state is application-scoped and remains available after this screen is recreated. */
  val taskState: StateFlow<ServerTaskRepositoryState>
    get() = EMPTY_TASK_STATE

  val taskNotifications: StateFlow<ServerTaskNotification?>
    get() = EMPTY_TASK_NOTIFICATIONS

  fun acknowledgeTaskNotification(taskId: String) = Unit

  suspend fun refreshTasks(): Result<Unit> = Result.success(Unit)

  suspend fun startScan(libraryId: String): Result<Unit> =
    Result.failure(UnsupportedOperationException("Library scanning is unavailable"))

  suspend fun startMatch(libraryId: String): Result<Unit> =
    Result.failure(UnsupportedOperationException("Book matching is unavailable"))

  suspend fun retryTaskSynchronization(taskId: String): Result<Unit> =
    Result.failure(UnsupportedOperationException("Task synchronization is unavailable"))

  /** Persists the requested order and returns the complete server-authoritative order. */
  suspend fun reorderLibraries(
    libraries: List<LibraryAdministrationLibrary>
  ): Result<List<LibraryAdministrationLibrary>> =
    Result.failure(UnsupportedOperationException("Library reorder is unavailable"))
}

enum class LibraryAdministrationConnectionState {
  UNKNOWN,
  CONNECTED,
  DISCONNECTED,
}

enum class LibraryAdministrationTaskState {
  UNKNOWN,
  IDLE,
  ACTIVE,
}
