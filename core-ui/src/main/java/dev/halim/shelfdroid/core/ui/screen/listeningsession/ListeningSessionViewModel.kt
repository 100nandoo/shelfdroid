package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionRepository
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ListeningSessionViewModel
@Inject
constructor(private val repository: ListeningSessionRepository) : ViewModel() {

  val uiState: StateFlow<ListeningSessionUiState> =
    flow { emit(repository.item()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ListeningSessionUiState())

  fun onEvent(event: ListeningSessionEvent) {
    when (event) {
      ListeningSessionEvent.OnInit -> {}
    }
  }
}

sealed interface ListeningSessionEvent {

  data object OnInit : ListeningSessionEvent
}
