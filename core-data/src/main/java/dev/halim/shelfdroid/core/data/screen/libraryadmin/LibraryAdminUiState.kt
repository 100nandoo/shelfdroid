package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.task.ServerTask
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification

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
  val connectionState: LibraryAdminConnectionState =
    LibraryAdminConnectionState.UNKNOWN,
  val taskStates: Map<String, LibraryAdminTaskState> = emptyMap(),
  val tasks: List<ServerTask> = emptyList(),
  val scanError: LibraryAdminError? = null,
  val matchError: LibraryAdminError? = null,
  val taskSyncError: LibraryAdminError? = null,
  val deleteError: LibraryAdminError? = null,
  val deleteSyncError: LibraryAdminError? = null,
  val deleteRetryLibraryId: String? = null,
  val deletingLibraryId: String? = null,
  val deleteConfirmationLibraryId: String? = null,
  val taskNotification: ServerTaskNotification? = null,
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
      it.id == libraryId &&
        it.mediaType != LibraryAdminMediaType.UNKNOWN
    } &&
    taskStates[libraryId] == LibraryAdminTaskState.IDLE

fun LibraryAdminUiState.canStartMatch(libraryId: String): Boolean =
  connectionState == LibraryAdminConnectionState.CONNECTED &&
    libraries.any {
      it.id == libraryId &&
        it.mediaType == LibraryAdminMediaType.BOOK
    } &&
    taskStates[libraryId] == LibraryAdminTaskState.IDLE

fun LibraryAdminUiState.canDelete(libraryId: String): Boolean =
  connectionState == LibraryAdminConnectionState.CONNECTED &&
    libraries.any { it.id == libraryId } &&
    taskStates[libraryId] == LibraryAdminTaskState.IDLE &&
    deletingLibraryId == null &&
    !isReordering

fun LibraryAdminUiState.taskForLibrary(libraryId: String): ServerTask? =
  tasks.firstOrNull { it.libraryId == libraryId && it.status == ServerTaskStatus.ACTIVE }
    ?: tasks.firstOrNull { it.libraryId == libraryId }

fun ServerTaskStatus.toAdministrationTaskState(): LibraryAdminTaskState =
  when (this) {
    ServerTaskStatus.ACTIVE -> LibraryAdminTaskState.ACTIVE
    ServerTaskStatus.COMPLETED,
    ServerTaskStatus.FAILED,
    ServerTaskStatus.CANCELLED -> LibraryAdminTaskState.IDLE
  }
