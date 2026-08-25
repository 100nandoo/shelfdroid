package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationConnectionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationTaskState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canReorder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

  private val _uiState = MutableStateFlow(LibraryAdministrationUiState())
  val uiState: StateFlow<LibraryAdministrationUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryAdministrationUiState(),
      )

  fun onEvent(event: LibraryAdministrationEvent) {
    when (event) {
      LibraryAdministrationEvent.Refresh -> load()
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
}

sealed interface LibraryAdministrationEvent {
  data object Refresh : LibraryAdministrationEvent

  data class MoveLibrary(val libraryId: String, val delta: Int) : LibraryAdministrationEvent

  data class MoveLibraryTo(val libraryId: String, val destinationIndex: Int) :
    LibraryAdministrationEvent

  data class SetConnectionState(val state: LibraryAdministrationConnectionState) :
    LibraryAdministrationEvent

  data class SetTaskState(val libraryId: String, val state: LibraryAdministrationTaskState) :
    LibraryAdministrationEvent

  data object ClearReorderError : LibraryAdministrationEvent
}
