package dev.halim.shelfdroid.core.ui.screen.apikeys.createedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.apikeys.createedit.CreateApiKeyFieldError
import dev.halim.shelfdroid.core.data.screen.apikeys.createedit.CreateEditApiKeysRepository
import dev.halim.shelfdroid.core.data.screen.apikeys.createedit.CreateEditApiKeysUiState
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
constructor(
  savedStateHandle: SavedStateHandle,
  private val repository: CreateEditApiKeysRepository,
) : ViewModel() {

  private val nav = savedStateHandle.toRoute<NavEditApiKeys>()
  val isCreateMode = nav.isCreateMode()

  private val _uiState = MutableStateFlow(repository.item(nav))

  val uiState: StateFlow<CreateEditApiKeysUiState> =
    _uiState.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      CreateEditApiKeysUiState(),
    )

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
    if (isCreateMode) {
      val validation = validateCreate()
      if (validation.hasError) {
        _uiState.update { it.copy(fieldError = validation) }
        _events.emit(GenericUiEvent.ShowErrorSnackbar())
        return@launch
      }
    }
    _uiState.update {
      it.copy(state = GenericState.Loading, fieldError = CreateApiKeyFieldError.None)
    }
    if (isCreateMode) {
      _uiState.update { repository.createApiKey(_uiState.value, _events) }
    } else {
      _uiState.update { repository.updateApiKey(_uiState.value, _events) }
    }
  }

  private fun validateCreate(): CreateApiKeyFieldError {
    val ui = _uiState.value
    return CreateApiKeyFieldError(
      nameEmpty = ui.name.isBlank(),
      userNotSelected = ui.selectedUserId.isBlank(),
      expiresAtEmpty = !ui.neverExpires && ui.expiresAtMillis == 0L,
    )
  }
}

sealed interface EditApiKeysEvent {
  data class Update(val transform: (CreateEditApiKeysUiState) -> CreateEditApiKeysUiState) :
    EditApiKeysEvent

  data object Submit : EditApiKeysEvent
}
