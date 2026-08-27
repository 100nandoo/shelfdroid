package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

private val EMPTY_TASK_STATE = MutableStateFlow(ServerTaskRepositoryState())
private val EMPTY_TASK_NOTIFICATIONS = MutableStateFlow<ServerTaskNotification?>(null)
private val EMPTY_LIBRARY_EVENTS = MutableSharedFlow<LibraryAdminLibraryEvent>().asSharedFlow()

interface LibraryAdminContract {
  suspend fun loadLibraries(): Result<List<LibraryAdminLibrary>>

  /** Reconciled changes received from other clients or from this client's mutation echo. */
  val libraryEvents: SharedFlow<LibraryAdminLibraryEvent>
    get() = EMPTY_LIBRARY_EVENTS

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

  /** Persists the requested order and reports whether follow-up Library data synchronization ran. */
  suspend fun reorderLibraries(
    libraries: List<LibraryAdminLibrary>
  ): Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>> =
    Result.failure(UnsupportedOperationException("Library reorder is unavailable"))

  /** Retries only Library data synchronization after an accepted mutation. */
  suspend fun synchronizeLibraries(): Result<Unit> = Result.success(Unit)

  /** Deletes a Library and reports whether follow-up Library data synchronization ran. */
  suspend fun deleteLibrary(
    libraryId: String
  ): Result<LibraryAdminMutationResult<Unit>> =
    Result.failure(UnsupportedOperationException("Library deletion is unavailable"))
}

enum class LibraryAdminConnectionState {
  UNKNOWN,
  CONNECTED,
  DISCONNECTED,
}

enum class LibraryAdminTaskState {
  UNKNOWN,
  IDLE,
  ACTIVE,
}
