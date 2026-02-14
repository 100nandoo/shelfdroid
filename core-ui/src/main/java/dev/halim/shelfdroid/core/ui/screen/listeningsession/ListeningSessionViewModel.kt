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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ListeningSessionViewModel
@Inject
constructor(private val repository: ListeningSessionRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(ListeningSessionUiState())
  val uiState: StateFlow<ListeningSessionUiState> =
    combine(_uiState, repository.listeningSessionPrefs) { state, prefs ->
        val userAndCountFilter = state.userAndCountFilter.copy(itemsPerPage = prefs.itemsPerPage)

        state.copy(userAndCountFilter = userAndCountFilter)
      }
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ListeningSessionUiState())

  fun onEvent(event: ListeningSessionEvent) {
    when (event) {
      ListeningSessionEvent.DeleteSessions -> {
        viewModelScope.launch {
          _uiState.update {
            val ids = it.selection.selectedIds
            repository.deleteSessions(it, ids)
          }
        }
      }

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
        _uiState.update { it.copy(pageInfo = _uiState.value.pageInfo.copy(inputPage = event.page)) }
      }

      is ListeningSessionEvent.FilterUser -> {
        _uiState.update {
          it.copy(
            userAndCountFilter = _uiState.value.userAndCountFilter.copy(selectedUser = event.user)
          )
        }
        fetchPage(0)
      }

      is ListeningSessionEvent.DeleteSession -> {
        viewModelScope.launch { _uiState.update { repository.deleteSession(it, event.session) } }
      }

      is ListeningSessionEvent.Select -> {
        _uiState.update {
          val selectedIds =
            if (it.selection.selectedIds.contains(event.id)) {
              it.selection.selectedIds - event.id
            } else {
              it.selection.selectedIds + event.id
            }

          val selection = it.selection.copy(selectedIds = selectedIds)
          it.copy(selection = selection)
        }
      }
      is ListeningSessionEvent.SelectionMode -> {
        _uiState.update { uiState ->
          val selection =
            if (event.isSelectionMode) {
              uiState.selection.copy(isSelectionMode = true, selectedIds = setOf(event.id))
            } else {
              uiState.selection.copy(isSelectionMode = false, selectedIds = emptySet())
            }
          uiState.copy(selection = selection)
        }
      }
    }
  }

  private fun initialPage() {
    loadingState()
    viewModelScope.launch {
      val result = repository.item(0)
      _uiState.value = result
    }
  }

  private fun fetchPage(targetPage: Int = _uiState.value.pageInfo.page) {
    loadingState()
    viewModelScope.launch {
      val result =
        repository.page(
          targetPage,
          repository.listeningSessionPrefs.first().itemsPerPage,
          uiState.value.userAndCountFilter.selectedUser.id,
          uiState.value.userAndCountFilter.users,
        )
      _uiState.value = result
    }
  }

  private fun loadingState() {
    _uiState.value = _uiState.value.copy(state = GenericState.Loading)
  }
}

sealed interface ListeningSessionEvent {

  data object DeleteSessions : ListeningSessionEvent

  data class ChangePage(val isNext: Boolean) : ListeningSessionEvent

  data class ChangeToPage(val page: Int) : ListeningSessionEvent

  data class ChangeInputPage(val page: Int) : ListeningSessionEvent

  data class FilterUser(val user: ListeningSessionUiState.User) : ListeningSessionEvent

  data class ChangeItemsPerPage(val itemsPerPage: ItemsPerPage) : ListeningSessionEvent

  data class DeleteSession(val session: ListeningSessionUiState.Session) : ListeningSessionEvent

  data class SelectionMode(val isSelectionMode: Boolean, val id: String) : ListeningSessionEvent

  data class Select(val id: String) : ListeningSessionEvent
}
