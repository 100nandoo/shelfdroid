package dev.halim.shelfdroid.core.ui.screen.serversettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.serversettings.ServerSettingsApiState
import dev.halim.shelfdroid.core.data.screen.serversettings.ServerSettingsRepository
import dev.halim.shelfdroid.core.data.screen.serversettings.ServerSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerSettingsViewModel
@Inject
constructor(private val repository: ServerSettingsRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(ServerSettingsUiState())
  val uiState: StateFlow<ServerSettingsUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ServerSettingsUiState())

  fun onEvent(event: ServerSettingsEvent) {
    when (event) {
      is ServerSettingsEvent.UpdateUiState -> _uiState.update { event.transform(it) }
      ServerSettingsEvent.Save -> saveSettings()
      ServerSettingsEvent.PurgeCache -> updateSettings { repository.purgeCache(it) }
      ServerSettingsEvent.PurgeItemsCache -> updateSettings { repository.purgeItemsCache(it) }
    }
  }

  private fun saveSettings() {
    viewModelScope.launch {
      _uiState.update { it.copy(apiState = ServerSettingsApiState.Loading) }
      _uiState.update { repository.saveSettings(it) }
    }
  }

  private fun updateSettings(update: suspend (ServerSettingsUiState) -> ServerSettingsUiState) {
    viewModelScope.launch {
      _uiState.update { it.copy(apiState = ServerSettingsApiState.Loading) }
      _uiState.update { update(it) }
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.loadSettings() } }
  }
}

sealed interface ServerSettingsEvent {
  data class UpdateUiState(val transform: (ServerSettingsUiState) -> ServerSettingsUiState) :
    ServerSettingsEvent

  data object Save : ServerSettingsEvent

  data object PurgeCache : ServerSettingsEvent

  data object PurgeItemsCache : ServerSettingsEvent
}
