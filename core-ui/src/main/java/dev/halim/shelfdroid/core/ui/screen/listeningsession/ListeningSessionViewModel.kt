package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ItemsPerPage
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionRepository
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ListeningSessionViewModel
@Inject
constructor(private val repository: ListeningSessionRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(ListeningSessionUiState())
  val uiState: StateFlow<ListeningSessionUiState> =
    combine(_uiState, repository.listeningSessionPrefs) { state, prefs ->
        state.copy(listeningSessionPrefs = prefs)
      }
      .onStart { fetchPage() }
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
      is ListeningSessionEvent.ChangeItemsPerPage -> {
        viewModelScope.launch {
          repository.updateItemsPerPage(event.itemsPerPage.label)
          fetchPage(0)
        }
      }

      is ListeningSessionEvent.ChangeInputPage -> {
        _uiState.value =
          _uiState.value.copy(pageInfo = _uiState.value.pageInfo.copy(inputPage = event.page))
      }
    }
  }

  private fun fetchPage(targetPage: Int = _uiState.value.pageInfo.page) {
    loadingState()
    viewModelScope.launch {
      val result =
        repository.item(targetPage, repository.listeningSessionPrefs.first().itemsPerPage)
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

  data class ChangeInputPage(val page: Int) : ListeningSessionEvent

  data class ChangeItemsPerPage(val itemsPerPage: ItemsPerPage) : ListeningSessionEvent
}
