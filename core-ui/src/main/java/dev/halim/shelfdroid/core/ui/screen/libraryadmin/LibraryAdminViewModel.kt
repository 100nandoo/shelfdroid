package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminConnectionState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminContract
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibraryEvent
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMutationResult
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminTaskState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canDelete
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canReorder
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canStartMatch
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canStartScan
import dev.halim.shelfdroid.core.data.screen.libraryadmin.toAdministrationTaskState
import dev.halim.shelfdroid.core.data.task.TaskConnectionState
import dev.halim.shelfdroid.core.data.task.TaskRepositoryState
import dev.halim.shelfdroid.core.data.task.TaskStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryAdminViewModel @Inject constructor(private val repository: LibraryAdminContract) :
  ViewModel() {

  private var loadGeneration = 0L
  private var intentGeneration = 0L
  private var lastAcceptedIntentGeneration = 0L
  private var lastServerLibraries: List<LibraryAdminLibrary> = emptyList()
  private var latestTaskState = TaskRepositoryState()

  private val _uiState = MutableStateFlow(LibraryAdminUiState())
  val uiState: StateFlow<LibraryAdminUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryAdminUiState(),
      )

  init {
    viewModelScope.launch {
      repository.taskState.collect { taskState ->
        latestTaskState = taskState
        applyTaskState(taskState, _uiState.value.libraries)
      }
    }
    viewModelScope.launch {
      repository.taskNotifications.collect { notification ->
        _uiState.update { it.copy(taskNotification = notification) }
      }
    }
    viewModelScope.launch {
      repository.libraryEvents.collect { event ->
        handleLibraryEvent(event)
      }
    }
  }

  fun consumeTaskNotification() {
    val notification = _uiState.value.taskNotification
    if (notification != null) repository.acknowledgeTaskNotification(notification.taskId)
    _uiState.update { it.copy(taskNotification = null) }
  }

  fun onEvent(event: LibraryAdminEvent) {
    when (event) {
      LibraryAdminEvent.Refresh -> load()
      is LibraryAdminEvent.StartScan -> startScan(event.libraryId)
      is LibraryAdminEvent.StartMatch -> startMatch(event.libraryId)
      is LibraryAdminEvent.RequestDeleteLibrary -> requestDeleteLibrary(event.libraryId)
      LibraryAdminEvent.CancelDeleteLibrary ->
        _uiState.update { it.copy(deleteConfirmationLibraryId = null) }
      LibraryAdminEvent.ConfirmDeleteLibrary -> confirmDeleteLibrary()
      LibraryAdminEvent.RetryDeleteLibrary -> retryDeleteLibrary()
      LibraryAdminEvent.RetryDeleteSynchronization -> retryDeleteSynchronization()
      is LibraryAdminEvent.RetryTaskSynchronization -> retryTaskSynchronization(event.taskId)
      is LibraryAdminEvent.MoveLibrary -> moveLibrary(event.libraryId, event.delta)
      is LibraryAdminEvent.MoveLibraryTo -> moveLibraryTo(event.libraryId, event.destinationIndex)
      LibraryAdminEvent.RetryReorderSynchronization -> retryReorderSynchronization()
      LibraryAdminEvent.ClearReorderError ->
        _uiState.update {
          it.copy(reorderError = null, reorderSyncError = null, reorderRetryOrder = null)
        }
    }
  }

  private fun load() {
    val requestGeneration = ++loadGeneration
    val requestIntentGeneration = intentGeneration
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          state = if (it.libraries.isEmpty()) GenericState.Loading else it.state,
          isRefreshing = true,
        )
      }
      repository.refreshTasks()
      repository
        .loadLibraries()
        .fold(
          onSuccess = { libraries ->
            if (requestGeneration != loadGeneration) return@fold
            if (requestIntentGeneration != intentGeneration) {
              _uiState.update { it.copy(isRefreshing = false) }
              return@fold
            }
            lastServerLibraries = libraries
            lastAcceptedIntentGeneration = intentGeneration
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                libraries = libraries,
                isRefreshing = false,
                reorderError = null,
                reorderSyncError = null,
                reorderRetryOrder = null,
                deleteSyncError = null,
                deleteRetryLibraryId = null,
              )
            }
            applyTaskState(latestTaskState, libraries)
          },
          onFailure = { error ->
            if (requestGeneration != loadGeneration) return@fold
            if (requestIntentGeneration != intentGeneration) {
              _uiState.update { it.copy(isRefreshing = false) }
              return@fold
            }
            _uiState.update {
              it.copy(
                // Synchronization failures may contain server or database details. The screen
                // supplies the localized generic message and keeps the refresh retry target.
                state = GenericState.Failure(null),
                isRefreshing = false,
              )
            }
          },
        )
    }
  }

  private fun handleLibraryEvent(event: LibraryAdminLibraryEvent) {
    if (!event.synchronized) {
      // The event payload is only a hint. Keep the server-authoritative list untouched and expose
      // the existing localized unavailable state; the screen's retry action performs a refresh.
      _uiState.update {
        it.copy(state = GenericState.Failure(null), isRefreshing = false)
      }
      return
    }

    // An event reconciliation is a complete server snapshot. Invalidate older HTTP responses and
    // optimistic intents so an out-of-order response cannot undo the externally accepted order.
    loadGeneration++
    intentGeneration++
    lastServerLibraries = event.libraries
    lastAcceptedIntentGeneration = intentGeneration
    _uiState.update {
      it.copy(
        state = GenericState.Success,
        libraries = event.libraries,
        isRefreshing = false,
        reorderError = null,
        reorderSyncError = null,
        reorderRetryOrder = null,
        deleteSyncError = null,
        deleteRetryLibraryId = null,
      )
    }
    applyTaskState(latestTaskState, event.libraries)
  }

  private fun applyTaskState(
    taskState: TaskRepositoryState,
    libraries: List<LibraryAdminLibrary>,
  ) {
    val taskStates = libraries.associate { library ->
      val libraryTasks = taskState.tasks.filter { it.libraryId == library.id }
      val active = libraryTasks.firstOrNull { it.status == TaskStatus.ACTIVE }
      val latest = libraryTasks.firstOrNull()
      val state =
        if (
          !taskState.snapshotKnown || taskState.connectionState != TaskConnectionState.CONNECTED
        ) {
          LibraryAdminTaskState.UNKNOWN
        } else {
          (active ?: latest)?.status?.toAdministrationTaskState() ?: LibraryAdminTaskState.IDLE
        }
      library.id to state
    }
    _uiState.update {
      it.copy(
        connectionState = taskState.connectionState.toAdministrationConnectionState(),
        taskStates = taskStates,
        tasks = taskState.tasks,
      )
    }
  }

  private fun moveLibrary(libraryId: String, delta: Int) {
    val libraries = _uiState.value.libraries
    val index = libraries.indexOfFirst { it.id == libraryId }
    if (index < 0) return
    moveLibraryTo(libraryId, (index + delta).coerceIn(0, libraries.lastIndex))
  }

  private fun moveLibraryTo(libraryId: String, destinationIndex: Int) {
    val current = _uiState.value
    if (!current.canReorder(libraryId)) return
    val sourceIndex = current.libraries.indexOfFirst { it.id == libraryId }
    if (sourceIndex < 0 || current.libraries.isEmpty()) return
    val targetIndex = destinationIndex.coerceIn(0, current.libraries.lastIndex)
    if (sourceIndex == targetIndex) return

    val reordered =
      current.libraries.toMutableList().apply {
        add(targetIndex, removeAt(sourceIndex))
      }
    val requestGeneration = ++intentGeneration
    _uiState.update {
      it.copy(
        libraries = reordered,
        isReordering = true,
        reorderError = null,
        reorderSyncError = null,
        reorderRetryOrder = null,
      )
    }

    viewModelScope.launch {
      repository
        .reorderLibraries(reordered)
        .fold(
          onSuccess = { outcome ->
            // Older acknowledgements must not overwrite a newer optimistic intent. The shared
            // mutation coordinator still serializes the requests on the server.
            if (
              requestGeneration != intentGeneration ||
                requestGeneration < lastAcceptedIntentGeneration
            ) {
              return@fold
            }
            when (outcome) {
              is LibraryAdminMutationResult.Accepted -> {
                lastServerLibraries = outcome.value
                lastAcceptedIntentGeneration = requestGeneration
                _uiState.update {
                  it.copy(
                    libraries = outcome.value,
                    isReordering = false,
                    reorderError = null,
                    reorderSyncError = null,
                    reorderRetryOrder = null,
                  )
                }
              }
              is LibraryAdminMutationResult.AcceptedButNotSynchronized -> {
                lastServerLibraries = outcome.value
                lastAcceptedIntentGeneration = requestGeneration
                _uiState.update {
                  it.copy(
                    // The server accepted this order. Keep it visible even though catalog data
                    // synchronization must be retried separately.
                    libraries = outcome.value,
                    isReordering = false,
                    reorderError = null,
                    reorderSyncError = LibraryAdminError.GenericReorderSynchronization,
                    reorderRetryOrder = outcome.value,
                  )
                }
              }
            }
          },
          onFailure = { error ->
            if (requestGeneration != intentGeneration) return@fold
            _uiState.update {
              it.copy(
                libraries = lastServerLibraries,
                isReordering = false,
                reorderError = error.message ?: "Library order could not be saved.",
                reorderSyncError = null,
                reorderRetryOrder = null,
              )
            }
          },
        )
    }
  }

  private fun retryReorderSynchronization() {
    val retryOrder = _uiState.value.reorderRetryOrder ?: return
    if (_uiState.value.isReordering) return
    val requestGeneration = intentGeneration
    _uiState.update { it.copy(isReordering = true, reorderSyncError = null) }
    viewModelScope.launch {
      repository
        .synchronizeLibraries()
        .fold(
          onSuccess = {
            if (requestGeneration != intentGeneration) return@fold
            _uiState.update { current ->
              if (current.reorderRetryOrder != retryOrder) {
                current
              } else {
                current.copy(
                  isReordering = false,
                  reorderSyncError = null,
                  reorderRetryOrder = null,
                )
              }
            }
          },
          onFailure = {
            if (requestGeneration != intentGeneration) return@fold
            _uiState.update { current ->
              current.copy(
                isReordering = false,
                reorderSyncError = LibraryAdminError.GenericReorderSynchronization,
                reorderRetryOrder = retryOrder,
              )
            }
          },
        )
    }
  }

  private fun startScan(libraryId: String) {
    if (!_uiState.value.canStartScan(libraryId)) return
    _uiState.update { it.copy(scanError = null) }
    viewModelScope.launch {
      repository.startScan(libraryId).onFailure { error ->
        _uiState.update { it.copy(scanError = error.safeMessage()) }
      }
    }
  }

  private fun startMatch(libraryId: String) {
    if (!_uiState.value.canStartMatch(libraryId)) return
    _uiState.update { it.copy(matchError = null) }
    viewModelScope.launch {
      repository.startMatch(libraryId).onFailure { error ->
        _uiState.update {
          it.copy(matchError = error.safeMessage(LibraryAdminError.GenericMatchStart))
        }
      }
    }
  }

  private fun retryTaskSynchronization(taskId: String) {
    _uiState.update { it.copy(taskSyncError = null) }
    viewModelScope.launch {
      repository.retryTaskSynchronization(taskId).onFailure { error ->
        _uiState.update {
          it.copy(taskSyncError = error.safeMessage(LibraryAdminError.GenericSynchronization))
        }
      }
    }
  }

  private fun requestDeleteLibrary(libraryId: String) {
    if (!_uiState.value.canDelete(libraryId)) return
    _uiState.update {
      it.copy(deleteConfirmationLibraryId = libraryId, deleteError = null)
    }
  }

  private fun confirmDeleteLibrary() {
    val current = _uiState.value
    val libraryId = current.deleteConfirmationLibraryId ?: return
    if (!current.canDelete(libraryId)) {
      _uiState.update { it.copy(deleteConfirmationLibraryId = null) }
      return
    }
    _uiState.update {
      it.copy(
        deleteConfirmationLibraryId = null,
        deletingLibraryId = libraryId,
        deleteError = null,
        deleteSyncError = null,
        deleteRetryLibraryId = null,
      )
    }
    viewModelScope.launch {
      repository
        .deleteLibrary(libraryId)
        .fold(
          onSuccess = { outcome ->
            val state = _uiState.value
            val remaining = state.libraries.filterNot { it.id == libraryId }
            lastServerLibraries = remaining
            lastAcceptedIntentGeneration = intentGeneration
            val synchronizationFailed =
              outcome is LibraryAdminMutationResult.AcceptedButNotSynchronized
            _uiState.update {
              it.copy(
                libraries = remaining,
                deletingLibraryId = null,
                deleteError = null,
                deleteSyncError =
                  if (synchronizationFailed) {
                    LibraryAdminError.GenericDeleteSynchronization
                  } else {
                    null
                  },
                deleteRetryLibraryId = if (synchronizationFailed) libraryId else null,
                taskStates = it.taskStates - libraryId,
                tasks = it.tasks.filterNot { task -> task.libraryId == libraryId },
              )
            }
          },
          onFailure = {
            _uiState.update {
              it.copy(
                deletingLibraryId = null,
                deleteError = LibraryAdminError.GenericDelete,
                deleteSyncError = null,
                deleteRetryLibraryId = libraryId,
              )
            }
          },
        )
    }
  }

  private fun retryDeleteLibrary() {
    val libraryId = _uiState.value.deleteRetryLibraryId ?: return
    if (!_uiState.value.canDelete(libraryId)) return
    _uiState.update { it.copy(deleteConfirmationLibraryId = libraryId, deleteError = null) }
  }

  private fun retryDeleteSynchronization() {
    val libraryId = _uiState.value.deleteRetryLibraryId ?: return
    if (_uiState.value.deleteSyncError == null || _uiState.value.deletingLibraryId != null) return
    val requestGeneration = intentGeneration
    _uiState.update {
      it.copy(
        deletingLibraryId = libraryId,
        deleteSyncError = null,
      )
    }
    viewModelScope.launch {
      repository
        .synchronizeLibraries()
        .fold(
          onSuccess = {
            if (requestGeneration != intentGeneration) return@fold
            _uiState.update { current ->
              if (current.deleteRetryLibraryId != libraryId) {
                current
              } else {
                current.copy(
                  deletingLibraryId = null,
                  deleteSyncError = null,
                  deleteRetryLibraryId = null,
                )
              }
            }
          },
          onFailure = {
            if (requestGeneration != intentGeneration) return@fold
            _uiState.update { current ->
              current.copy(
                deletingLibraryId = null,
                deleteSyncError = LibraryAdminError.GenericDeleteSynchronization,
                deleteRetryLibraryId = libraryId,
              )
            }
          },
        )
    }
  }
}

private fun TaskConnectionState.toAdministrationConnectionState(): LibraryAdminConnectionState =
  when (this) {
    TaskConnectionState.UNKNOWN -> LibraryAdminConnectionState.UNKNOWN
    TaskConnectionState.CONNECTED -> LibraryAdminConnectionState.CONNECTED
    TaskConnectionState.DISCONNECTED -> LibraryAdminConnectionState.DISCONNECTED
  }

private fun Throwable.safeMessage(
  generic: LibraryAdminError = LibraryAdminError.GenericScanStart
): LibraryAdminError = generic

sealed interface LibraryAdminEvent {
  data object Refresh : LibraryAdminEvent

  data class StartScan(val libraryId: String) : LibraryAdminEvent

  data class StartMatch(val libraryId: String) : LibraryAdminEvent

  data class RequestDeleteLibrary(val libraryId: String) : LibraryAdminEvent

  data object CancelDeleteLibrary : LibraryAdminEvent

  data object ConfirmDeleteLibrary : LibraryAdminEvent

  data object RetryDeleteLibrary : LibraryAdminEvent

  data object RetryDeleteSynchronization : LibraryAdminEvent

  data class RetryTaskSynchronization(val taskId: String) : LibraryAdminEvent

  data class MoveLibrary(val libraryId: String, val delta: Int) : LibraryAdminEvent

  data class MoveLibraryTo(val libraryId: String, val destinationIndex: Int) : LibraryAdminEvent

  data object RetryReorderSynchronization : LibraryAdminEvent

  data object ClearReorderError : LibraryAdminEvent
}
