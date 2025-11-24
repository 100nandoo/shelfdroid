package dev.halim.shelfdroid.core.ui.screen.podcastfeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedRepository
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastFeedViewModel
@Inject
constructor(savedStateHandle: SavedStateHandle, private val repository: PodcastFeedRepository) :
  ViewModel() {
  val libraryId: String = checkNotNull(savedStateHandle.get<String>("libraryId"))
  val rssFeed: String = checkNotNull(savedStateHandle.get<String>("rssFeed"))

  private val _uiState = MutableStateFlow(PodcastFeedUiState())

  val uiState: StateFlow<PodcastFeedUiState> = _uiState

  init {
    viewModelScope.launch { _uiState.update { repository.feed(rssFeed) } }
  }
}
