package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.task.ServerTask
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification

sealed interface LibraryAdministrationError {
  data class SafeMessage(val message: String) : LibraryAdministrationError

  data object GenericScanStart : LibraryAdministrationError

  data object GenericMatchStart : LibraryAdministrationError

  data object GenericSynchronization : LibraryAdministrationError

  data object GenericDelete : LibraryAdministrationError
}

data class LibraryAdministrationUiState(
  val state: GenericState = GenericState.Loading,
  val libraries: List<LibraryAdministrationLibrary> = emptyList(),
  val isRefreshing: Boolean = true,
  val connectionState: LibraryAdministrationConnectionState =
    LibraryAdministrationConnectionState.UNKNOWN,
  val taskStates: Map<String, LibraryAdministrationTaskState> = emptyMap(),
  val tasks: List<ServerTask> = emptyList(),
  val scanError: LibraryAdministrationError? = null,
  val matchError: LibraryAdministrationError? = null,
  val taskSyncError: LibraryAdministrationError? = null,
  val deleteError: LibraryAdministrationError? = null,
  val deleteRetryLibraryId: String? = null,
  val deletingLibraryId: String? = null,
  val deleteConfirmationLibraryId: String? = null,
  val taskNotification: ServerTaskNotification? = null,
  val reorderError: String? = null,
  val isReordering: Boolean = false,
)

fun LibraryAdministrationUiState.canReorder(libraryId: String): Boolean =
  connectionState == LibraryAdministrationConnectionState.CONNECTED &&
    libraries.any { it.id == libraryId } &&
    deletingLibraryId == null &&
    deleteConfirmationLibraryId == null &&
    // Moving one row also changes every row it crosses. Require a known idle snapshot for the
    // whole ordered set so an active/unknown library cannot be shifted indirectly.
    libraries.all { taskStates[it.id] == LibraryAdministrationTaskState.IDLE }

fun LibraryAdministrationUiState.canStartScan(libraryId: String): Boolean =
  connectionState == LibraryAdministrationConnectionState.CONNECTED &&
    libraries.any {
      it.id == libraryId &&
        it.mediaType != LibraryAdministrationMediaType.UNKNOWN
    } &&
    taskStates[libraryId] == LibraryAdministrationTaskState.IDLE

fun LibraryAdministrationUiState.canStartMatch(libraryId: String): Boolean =
  connectionState == LibraryAdministrationConnectionState.CONNECTED &&
    libraries.any {
      it.id == libraryId &&
        it.mediaType == LibraryAdministrationMediaType.BOOK
    } &&
    taskStates[libraryId] == LibraryAdministrationTaskState.IDLE

fun LibraryAdministrationUiState.canDelete(libraryId: String): Boolean =
  connectionState == LibraryAdministrationConnectionState.CONNECTED &&
    libraries.any { it.id == libraryId } &&
    taskStates[libraryId] == LibraryAdministrationTaskState.IDLE &&
    deletingLibraryId == null &&
    !isReordering

fun LibraryAdministrationUiState.taskForLibrary(libraryId: String): ServerTask? =
  tasks.firstOrNull { it.libraryId == libraryId && it.status == ServerTaskStatus.ACTIVE }
    ?: tasks.firstOrNull { it.libraryId == libraryId }

fun ServerTaskStatus.toAdministrationTaskState(): LibraryAdministrationTaskState =
  when (this) {
    ServerTaskStatus.ACTIVE -> LibraryAdministrationTaskState.ACTIVE
    ServerTaskStatus.COMPLETED,
    ServerTaskStatus.FAILED,
    ServerTaskStatus.CANCELLED -> LibraryAdministrationTaskState.IDLE
  }
