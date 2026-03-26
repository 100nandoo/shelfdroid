package dev.halim.shelfdroid.core.ui.screen.apikeys.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.apikeys.edit.EditApiKeysRepository
import dev.halim.shelfdroid.core.data.screen.apikeys.edit.EditApiKeysUiState
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
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
class EditApiKeysViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: EditApiKeysRepository) :
  ViewModel() {

  private val _uiState =
    MutableStateFlow(repository.item(savedStateHandle.toRoute<NavEditApiKeys>()))

  val uiState: StateFlow<EditApiKeysUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EditApiKeysUiState())

  private val _events = MutableSharedFlow<GenericUiEvent>()
  val events = _events.asSharedFlow()

  fun onEvent(event: EditApiKeysEvent) {
    when (event) {
      is EditApiKeysEvent.Update -> {
        _uiState.update { event.transform(it) }
      }
      EditApiKeysEvent.Submit -> submit()
    }
  }

  private fun submit() = viewModelScope.launch {
    _uiState.update { it.copy(state = GenericState.Loading) }
    _uiState.update { repository.updateApiKey(_uiState.value, _events) }
  }
}

sealed interface EditApiKeysEvent {
  data class Update(val transform: (EditApiKeysUiState) -> EditApiKeysUiState) : EditApiKeysEvent

  data object Submit : EditApiKeysEvent
}
