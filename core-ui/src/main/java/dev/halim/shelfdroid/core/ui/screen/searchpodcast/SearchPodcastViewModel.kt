package dev.halim.shelfdroid.core.ui.screen.searchpodcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastRepository
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUiState
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SearchPodcastViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: SearchPodcastRepository) :
  ViewModel() {
  val libraryId: String = checkNotNull(savedStateHandle.get<String>("libraryId"))

  private val _uiState = MutableStateFlow(SearchPodcastUiState())

  val uiState: StateFlow<SearchPodcastUiState> = _uiState

  fun onEvent(event: SearchPodcastEvent) {
    when (event) {
      is SearchPodcastEvent.Search -> {
        _uiState.update { it.copy(state = SearchState.Loading) }
        viewModelScope.launch { _uiState.update { repository.search(event.term, libraryId) } }
      }
    }
  }
}

sealed class SearchPodcastEvent {
  data class Search(val term: String) : SearchPodcastEvent()
}
