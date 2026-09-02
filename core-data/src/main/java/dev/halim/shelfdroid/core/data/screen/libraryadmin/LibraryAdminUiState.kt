package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.task.Task
import dev.halim.shelfdroid.core.data.task.TaskNotification
import dev.halim.shelfdroid.core.data.task.TaskStatus

sealed interface LibraryAdminError {
  data class SafeMessage(val message: String) : LibraryAdminError

  data object GenericScanStart : LibraryAdminError

  data object GenericMatchStart : LibraryAdminError

  data object GenericSynchronization : LibraryAdminError

  data object GenericDelete : LibraryAdminError

  data object GenericDeleteSynchronization : LibraryAdminError

  data object GenericReorderSynchronization : LibraryAdminError
}

data class LibraryAdminUiState(
  val state: GenericState = GenericState.Loading,
  val libraries: List<LibraryAdminLibrary> = emptyList(),
  val isRefreshing: Boolean = true,
  val connectionState: LibraryAdminConnectionState = LibraryAdminConnectionState.UNKNOWN,
  val taskStates: Map<String, LibraryAdminTaskState> = emptyMap(),
  val tasks: List<Task> = emptyList(),
  val scanError: LibraryAdminError? = null,
  val matchError: LibraryAdminError? = null,
  val taskSyncError: LibraryAdminError? = null,
  val deleteError: LibraryAdminError? = null,
  val deleteSyncError: LibraryAdminError? = null,
  val deleteRetryLibraryId: String? = null,
  val deletingLibraryId: String? = null,
  val deleteConfirmationLibraryId: String? = null,
  val taskNotification: TaskNotification? = null,
  val reorderError: String? = null,
  val reorderSyncError: LibraryAdminError? = null,
  val reorderRetryOrder: List<LibraryAdminLibrary>? = null,
  val isReordering: Boolean = false,
)

fun LibraryAdminUiState.canReorder(libraryId: String): Boolean =
  connectionState == LibraryAdminConnectionState.CONNECTED &&
    libraries.any { it.id == libraryId } &&
    deletingLibraryId == null &&
    deleteConfirmationLibraryId == null &&
    // Moving one row also changes every row it crosses. Require a known idle snapshot for the
    // whole ordered set so an active/unknown library cannot be shifted indirectly.
    libraries.all { taskStates[it.id] == LibraryAdminTaskState.IDLE }

fun LibraryAdminUiState.canStartScan(libraryId: String): Boolean =
  connectionState == LibraryAdminConnectionState.CONNECTED &&
    libraries.any {
      it.id == libraryId && it.mediaType != MediaType.UNKNOWN
    } &&
    taskStates[libraryId] == LibraryAdminTaskState.IDLE

fun LibraryAdminUiState.canStartMatch(libraryId: String): Boolean =
  connectionState == LibraryAdminConnectionState.CONNECTED &&
    libraries.any {
      it.id == libraryId && it.mediaType == MediaType.BOOK
    } &&
    taskStates[libraryId] == LibraryAdminTaskState.IDLE

fun LibraryAdminUiState.canDelete(libraryId: String): Boolean =
  connectionState == LibraryAdminConnectionState.CONNECTED &&
    libraries.any { it.id == libraryId } &&
    taskStates[libraryId] == LibraryAdminTaskState.IDLE &&
    deletingLibraryId == null &&
    !isReordering

fun LibraryAdminUiState.taskForLibrary(libraryId: String): Task? =
  tasks.firstOrNull { it.libraryId == libraryId && it.status == TaskStatus.ACTIVE }
    ?: tasks.firstOrNull { it.libraryId == libraryId }

fun TaskStatus.toAdministrationTaskState(): LibraryAdminTaskState =
  when (this) {
    TaskStatus.ACTIVE -> LibraryAdminTaskState.ACTIVE
    TaskStatus.COMPLETED,
    TaskStatus.FAILED,
    TaskStatus.CANCELLED -> LibraryAdminTaskState.IDLE
  }
