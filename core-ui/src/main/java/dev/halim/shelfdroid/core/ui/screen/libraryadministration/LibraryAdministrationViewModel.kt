package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
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
    }
  }

  private fun load() {
    viewModelScope.launch {
      _uiState.update { it.copy(state = GenericState.Loading, isRefreshing = true) }
      repository.loadLibraries().fold(
        onSuccess = { libraries ->
          _uiState.update {
            it.copy(
              state = GenericState.Success,
              libraries = libraries,
              isRefreshing = false,
            )
          }
        },
        onFailure = { error ->
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
}

sealed interface LibraryAdministrationEvent {
  data object Refresh : LibraryAdministrationEvent
}
