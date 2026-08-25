package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationConnectionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationTaskState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canReorder
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canDelete
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canStartMatch
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canStartScan
import dev.halim.shelfdroid.core.data.screen.libraryadministration.toAdministrationTaskState
import dev.halim.shelfdroid.core.data.task.ServerTaskConnectionState
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
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
class LibraryAdministrationViewModel
@Inject
constructor(private val repository: LibraryAdministrationContract) : ViewModel() {

  private var loadGeneration = 0L
  private var intentGeneration = 0L
  private var lastAcceptedIntentGeneration = 0L
  private var lastServerLibraries: List<LibraryAdministrationLibrary> = emptyList()
  private var latestTaskState = ServerTaskRepositoryState()

  private val _uiState = MutableStateFlow(LibraryAdministrationUiState())
  val uiState: StateFlow<LibraryAdministrationUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryAdministrationUiState(),
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
  }

  fun consumeTaskNotification() {
    val notification = _uiState.value.taskNotification
    if (notification != null) repository.acknowledgeTaskNotification(notification.taskId)
    _uiState.update { it.copy(taskNotification = null) }
  }

  fun onEvent(event: LibraryAdministrationEvent) {
    when (event) {
      LibraryAdministrationEvent.Refresh -> load()
      is LibraryAdministrationEvent.StartScan -> startScan(event.libraryId)
      is LibraryAdministrationEvent.StartMatch -> startMatch(event.libraryId)
      is LibraryAdministrationEvent.RequestDeleteLibrary -> requestDeleteLibrary(event.libraryId)
      LibraryAdministrationEvent.CancelDeleteLibrary ->
        _uiState.update { it.copy(deleteConfirmationLibraryId = null) }
      LibraryAdministrationEvent.ConfirmDeleteLibrary -> confirmDeleteLibrary()
      LibraryAdministrationEvent.RetryDeleteLibrary ->
        retryDeleteLibrary()
      is LibraryAdministrationEvent.RetryTaskSynchronization ->
        retryTaskSynchronization(event.taskId)
      is LibraryAdministrationEvent.MoveLibrary -> moveLibrary(event.libraryId, event.delta)
      is LibraryAdministrationEvent.MoveLibraryTo ->
        moveLibraryTo(event.libraryId, event.destinationIndex)
      is LibraryAdministrationEvent.SetConnectionState ->
        _uiState.update { it.copy(connectionState = event.state) }
      is LibraryAdministrationEvent.SetTaskState ->
        _uiState.update { it.copy(taskStates = it.taskStates + (event.libraryId to event.state)) }
      LibraryAdministrationEvent.ClearReorderError ->
        _uiState.update { it.copy(reorderError = null) }
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
      repository.loadLibraries().fold(
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
              state = GenericState.Failure(error.message),
              isRefreshing = false,
            )
          }
        },
      )
    }
  }

  private fun applyTaskState(
    taskState: ServerTaskRepositoryState,
    libraries: List<LibraryAdministrationLibrary>,
  ) {
    val taskStates =
      libraries.associate { library ->
        val libraryTasks = taskState.tasks.filter { it.libraryId == library.id }
        val active = libraryTasks.firstOrNull { it.status == ServerTaskStatus.ACTIVE }
        val latest = libraryTasks.firstOrNull()
        val state =
          if (!taskState.snapshotKnown ||
            taskState.connectionState != ServerTaskConnectionState.CONNECTED
          ) {
            LibraryAdministrationTaskState.UNKNOWN
          } else {
            (active ?: latest)?.status?.toAdministrationTaskState()
              ?: LibraryAdministrationTaskState.IDLE
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

    val reordered = current.libraries.toMutableList().apply {
      add(targetIndex, removeAt(sourceIndex))
    }
    val requestGeneration = ++intentGeneration
    _uiState.update {
      it.copy(libraries = reordered, isReordering = true, reorderError = null)
    }

    viewModelScope.launch {
      repository.reorderLibraries(reordered).fold(
        onSuccess = { acceptedOrder ->
          // Older acknowledgements must not overwrite a newer optimistic intent. The shared
          // mutation coordinator still serializes the requests on the server.
          if (requestGeneration < lastAcceptedIntentGeneration) return@fold
          lastServerLibraries = acceptedOrder
          lastAcceptedIntentGeneration = requestGeneration
          _uiState.update {
            it.copy(
              libraries = if (requestGeneration == intentGeneration) acceptedOrder else it.libraries,
              isReordering = requestGeneration != intentGeneration,
            )
          }
        },
        onFailure = { error ->
          if (requestGeneration != intentGeneration) return@fold
          _uiState.update {
            it.copy(
              libraries = lastServerLibraries,
              isReordering = false,
              reorderError = error.message ?: "Library order could not be saved.",
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
        _uiState.update { it.copy(matchError = error.safeMessage(LibraryAdministrationError.GenericMatchStart)) }
      }
    }
  }

  private fun retryTaskSynchronization(taskId: String) {
    _uiState.update { it.copy(taskSyncError = null) }
    viewModelScope.launch {
      repository.retryTaskSynchronization(taskId).onFailure { error ->
        _uiState.update {
          it.copy(taskSyncError = error.safeMessage(LibraryAdministrationError.GenericSynchronization))
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
        deleteRetryLibraryId = null,
      )
    }
    viewModelScope.launch {
      repository.deleteLibrary(libraryId).fold(
        onSuccess = {
          val state = _uiState.value
          val remaining = state.libraries.filterNot { it.id == libraryId }
          lastServerLibraries = remaining
          lastAcceptedIntentGeneration = intentGeneration
          _uiState.update {
            it.copy(
              libraries = remaining,
              deletingLibraryId = null,
              deleteError = null,
              deleteRetryLibraryId = null,
              taskStates = it.taskStates - libraryId,
              tasks = it.tasks.filterNot { task -> task.libraryId == libraryId },
            )
          }
        },
        onFailure = {
          _uiState.update {
            it.copy(
              deletingLibraryId = null,
              deleteError = LibraryAdministrationError.GenericDelete,
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
}

private fun ServerTaskConnectionState.toAdministrationConnectionState():
  LibraryAdministrationConnectionState =
  when (this) {
    ServerTaskConnectionState.UNKNOWN -> LibraryAdministrationConnectionState.UNKNOWN
    ServerTaskConnectionState.CONNECTED -> LibraryAdministrationConnectionState.CONNECTED
    ServerTaskConnectionState.DISCONNECTED -> LibraryAdministrationConnectionState.DISCONNECTED
  }

private fun Throwable.safeMessage(
  generic: LibraryAdministrationError = LibraryAdministrationError.GenericScanStart
): LibraryAdministrationError = generic

sealed interface LibraryAdministrationEvent {
  data object Refresh : LibraryAdministrationEvent

  data class StartScan(val libraryId: String) : LibraryAdministrationEvent

  data class StartMatch(val libraryId: String) : LibraryAdministrationEvent

  data class RequestDeleteLibrary(val libraryId: String) : LibraryAdministrationEvent

  data object CancelDeleteLibrary : LibraryAdministrationEvent

  data object ConfirmDeleteLibrary : LibraryAdministrationEvent

  data object RetryDeleteLibrary : LibraryAdministrationEvent

  data class RetryTaskSynchronization(val taskId: String) : LibraryAdministrationEvent

  data class MoveLibrary(val libraryId: String, val delta: Int) : LibraryAdministrationEvent

  data class MoveLibraryTo(val libraryId: String, val destinationIndex: Int) :
    LibraryAdministrationEvent

  data class SetConnectionState(val state: LibraryAdministrationConnectionState) :
    LibraryAdministrationEvent

  data class SetTaskState(val libraryId: String, val state: LibraryAdministrationTaskState) :
    LibraryAdministrationEvent

  data object ClearReorderError : LibraryAdministrationEvent
}
