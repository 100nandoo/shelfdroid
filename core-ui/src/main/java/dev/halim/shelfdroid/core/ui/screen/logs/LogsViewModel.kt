package dev.halim.shelfdroid.core.ui.screen.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.core.data.screen.logs.LogsRepository
import dev.halim.shelfdroid.core.data.screen.logs.LogsUiEvent
import dev.halim.shelfdroid.core.data.screen.logs.LogsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LogsViewModel @Inject constructor(private val repository: LogsRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(LogsUiState())
  val uiState: StateFlow<LogsUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LogsUiState())

  private val _events = MutableSharedFlow<LogsUiEvent>()
  val events = _events.asSharedFlow()

  init {
    viewModelScope.launch { _uiState.update { repository.item(_events) } }
  }

  fun onEvent(event: LogsEvent) {
    when (event) {
      is LogsEvent.ChangeLogLevel -> {
        viewModelScope.launch {
          repository.changeServerLogLevel(_uiState.value, _events, event.logLevel)
        }
      }
      is LogsEvent.UpdateUiState -> _uiState.update { event.transform(it) }
    }
  }
}

sealed interface LogsEvent {
  data class UpdateUiState(val transform: (LogsUiState) -> LogsUiState) : LogsEvent

  data class ChangeLogLevel(val logLevel: LogLevel) : LogsEvent
}
