package dev.halim.shelfdroid.core.ui.screen.apikeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeysRepository
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeysUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ApiKeysViewModel @Inject constructor(private val repository: ApiKeysRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(ApiKeysUiState())
  val uiState: StateFlow<ApiKeysUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ApiKeysUiState())

  fun onEvent(event: ApiKeysEvent) {
    when (event) {
      ApiKeysEvent.OnInit -> initialPage()
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.apiKeys() } }
  }
}

sealed interface ApiKeysEvent {

  data object OnInit : ApiKeysEvent
}
