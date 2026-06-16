package dev.halim.shelfdroid.core.ui.screen.searchpodcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastRepository
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUiState
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.ui.navigation.SearchPodcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SearchPodcastViewModel.Factory::class)
class SearchPodcastViewModel
@AssistedInject
constructor(
  @Assisted navKey: SearchPodcast,
  private val repository: SearchPodcastRepository,
) : ViewModel() {
  val libraryId: String = navKey.libraryId
  private val _uiState = MutableStateFlow(SearchPodcastUiState())

  val uiState: StateFlow<SearchPodcastUiState> = _uiState

  fun onEvent(event: SearchPodcastEvent) {
    when (event) {
      is SearchPodcastEvent.Search -> {
        _uiState.update { it.copy(state = GenericState.Loading) }
        viewModelScope.launch { _uiState.update { repository.search(event.term, libraryId) } }
      }

      is SearchPodcastEvent.Update -> {
        _uiState.update {
          val updatedResult =
            it.result.map { searchPodcastUi ->
              if (searchPodcastUi.feedUrl == event.result.feedUrl) {
                searchPodcastUi.copy(id = event.result.id, isAdded = true)
              } else {
                searchPodcastUi
              }
            }
          it.copy(result = updatedResult)
        }
      }
    }
  }

  @AssistedFactory interface Factory {
    fun create(navKey: SearchPodcast): SearchPodcastViewModel
  }
}

sealed interface SearchPodcastEvent {
  data class Search(val term: String) : SearchPodcastEvent

  data class Update(val result: CreatePodcastNavResult) : SearchPodcastEvent
}
