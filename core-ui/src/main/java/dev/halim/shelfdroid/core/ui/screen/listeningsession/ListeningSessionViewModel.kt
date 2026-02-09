package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionRepository
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ListeningSessionViewModel
@Inject
constructor(private val repository: ListeningSessionRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(ListeningSessionUiState())
  val uiState: StateFlow<ListeningSessionUiState> =
    _uiState
      .onStart { viewModelScope.launch { _uiState.value = repository.item() } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ListeningSessionUiState())

  fun onEvent(event: ListeningSessionEvent) {
    when (event) {
      is ListeningSessionEvent.ChangePage -> {
        val targetPage = uiState.value.pageInfo.page + if (event.isNext) 1 else -1
        fetchPage(targetPage)
      }
      is ListeningSessionEvent.ChangeToPage -> {
        fetchPage(event.page)
      }
    }
  }

  private fun fetchPage(targetPage: Int) {
    loadingState()
    viewModelScope.launch {
      val result = repository.item(targetPage)
      _uiState.value = result
    }
  }

  private fun loadingState() {
    _uiState.value = _uiState.value.copy(state = GenericState.Loading)
  }
}

sealed interface ListeningSessionEvent {

  data class ChangePage(val isNext: Boolean) : ListeningSessionEvent

  data class ChangeToPage(val page: Int) : ListeningSessionEvent
}
