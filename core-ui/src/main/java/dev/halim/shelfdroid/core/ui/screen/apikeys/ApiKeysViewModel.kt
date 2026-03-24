package dev.halim.shelfdroid.core.ui.screen.apikeys

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeysRepository
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeysUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ApiKeysViewModel @Inject constructor(private val repository: ApiKeysRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(ApiKeysUiState())
  val uiState: StateFlow<ApiKeysUiState> = _uiState.asStateFlow()

  fun onEvent(event: ApiKeysEvent) {
    when (event) {
      ApiKeysEvent.OnInit -> {}
    }
  }
}

sealed interface ApiKeysEvent {

  data object OnInit : ApiKeysEvent
}
