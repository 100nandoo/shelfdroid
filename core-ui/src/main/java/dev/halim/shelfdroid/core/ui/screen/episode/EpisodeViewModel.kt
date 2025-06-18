package dev.halim.shelfdroid.core.ui.screen.episode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.episode.EpisodeRepository
import dev.halim.shelfdroid.core.data.screen.episode.EpisodeUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeViewModel
@Inject
constructor(
  private val savedStateHandle: SavedStateHandle,
  private val repository: EpisodeRepository,
) : ViewModel() {

  val itemId: String = checkNotNull(savedStateHandle.get<String>("itemId"))
  val episodeId: String = checkNotNull(savedStateHandle.get<String>("episodeId"))

  private val _uiState = MutableStateFlow(EpisodeUiState())
  val uiState: StateFlow<EpisodeUiState> =
    _uiState
      .onStart { initUiState() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EpisodeUiState())

  private fun initUiState() {
    viewModelScope.launch { _uiState.update { repository.item(itemId, episodeId) } }
  }
}
