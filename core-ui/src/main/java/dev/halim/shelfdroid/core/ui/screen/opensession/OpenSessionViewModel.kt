package dev.halim.shelfdroid.core.ui.screen.opensession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.opensession.OpenSessionRepository
import dev.halim.shelfdroid.core.data.screen.opensession.OpenSessionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OpenSessionViewModel @Inject constructor(private val repository: OpenSessionRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(OpenSessionUiState())
  val uiState: StateFlow<OpenSessionUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), OpenSessionUiState())

  fun onEvent(event: OpenSessionEvent) {
    when (event) {
      is OpenSessionEvent.CloseSession -> {
        viewModelScope.launch {
          _uiState.update { state -> repository.closeSession(event.id, state) }
        }
      }
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.openSessions() } }
  }
}

sealed interface OpenSessionEvent {

  data class CloseSession(val id: String) : OpenSessionEvent
}
