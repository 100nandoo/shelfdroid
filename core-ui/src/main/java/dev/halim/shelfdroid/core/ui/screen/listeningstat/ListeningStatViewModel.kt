package dev.halim.shelfdroid.core.ui.screen.listeningstat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.listeningstat.ListeningStatRepository
import dev.halim.shelfdroid.core.data.screen.listeningstat.ListeningStatUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ListeningStatViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, repository: ListeningStatRepository) : ViewModel() {
  val userId: String = checkNotNull(savedStateHandle.get<String>("userId"))

  private val _uiState = MutableStateFlow(repository.item(userId))
  val uiState: StateFlow<ListeningStatUiState> = _uiState.asStateFlow()

  fun onEvent(event: ListeningStatEvent) {
    when (event) {
      ListeningStatEvent.OnInit -> {}
    }
  }
}

sealed interface ListeningStatEvent {

  data object OnInit : ListeningStatEvent
}
