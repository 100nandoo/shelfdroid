package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.podcast.PodcastRepository
import dev.halim.shelfdroid.core.data.podcast.PodcastUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PodcastViewModel
@Inject
constructor(private val podcastRepository: PodcastRepository, savedStateHandle: SavedStateHandle) :
  ViewModel() {
  private val id: String = checkNotNull(savedStateHandle.get<String>("id"))

  private val _uiState = MutableStateFlow(PodcastUiState())
  val uiState: StateFlow<PodcastUiState> =
    _uiState
      .onStart { initUiState() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PodcastUiState())

  private fun initUiState() {
    viewModelScope.launch { _uiState.update { podcastRepository.item(id) } }
  }
}
