package dev.halim.shelfdroid.core.ui.screen.rssfeeds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsApiState
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsRepository
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RssFeedsViewModel @Inject constructor(private val repository: RssFeedsRepository) :
  ViewModel() {

  private val _uiState = MutableStateFlow(RssFeedsUiState())
  val uiState: StateFlow<RssFeedsUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), RssFeedsUiState())

  fun onEvent(event: RssFeedsEvent) {
    when (event) {
      is RssFeedsEvent.CloseFeed -> {
        viewModelScope.launch {
          _uiState.update { it.copy(apiState = RssFeedsApiState.Loading) }
          _uiState.update { repository.closeFeed(event.feedId, it) }
        }
      }
      RssFeedsEvent.Refresh -> initialPage()
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.loadFeeds() } }
  }
}

sealed interface RssFeedsEvent {
  data object Refresh : RssFeedsEvent

  data class CloseFeed(val feedId: String) : RssFeedsEvent
}
