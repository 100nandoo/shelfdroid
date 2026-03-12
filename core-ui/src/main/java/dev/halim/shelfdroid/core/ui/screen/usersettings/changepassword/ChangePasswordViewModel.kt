package dev.halim.shelfdroid.core.ui.screen.usersettings.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.changepassword.ChangePasswordRepository
import dev.halim.shelfdroid.core.data.screen.usersettings.changepassword.ChangePasswordUiEvent
import dev.halim.shelfdroid.core.data.screen.usersettings.changepassword.ChangePasswordUiState
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
class ChangePasswordViewModel
@Inject
constructor(private val repository: ChangePasswordRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(ChangePasswordUiState())
  val uiState: StateFlow<ChangePasswordUiState> =
    _uiState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ChangePasswordUiState())

  private val _events = MutableSharedFlow<ChangePasswordUiEvent>()
  val events = _events.asSharedFlow()

  fun onEvent(event: ChangePasswordEvent) {
    when (event) {
      is ChangePasswordEvent.Update -> {
        _uiState.update { event.transform(it) }
      }
      ChangePasswordEvent.Submit -> {
        validate()
      }
    }
  }

  private fun validate() {
    val new = _uiState.value.new
    val confirm = _uiState.value.confirm
    if (new != confirm) {
      viewModelScope.launch { _events.emit(ChangePasswordUiEvent.NotMatchError) }
      _uiState.update { it.copy(state = GenericState.Failure()) }
    } else {
      viewModelScope.launch { _uiState.update { it.copy(state = GenericState.Loading) } }
      viewModelScope.launch { _uiState.update { repository.changePassword(it, _events) } }
    }
  }
}

sealed interface ChangePasswordEvent {

  data class Update(val transform: (ChangePasswordUiState) -> ChangePasswordUiState) :
    ChangePasswordEvent

  data object Submit : ChangePasswordEvent
}
