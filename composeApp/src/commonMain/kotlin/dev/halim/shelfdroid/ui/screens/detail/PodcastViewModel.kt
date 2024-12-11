package dev.halim.shelfdroid.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.db.model.Episode
import dev.halim.shelfdroid.repo.PodcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PodcastViewModel(private val podcastRepository: PodcastRepository, private val id: String) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState
        .onStart { initUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PodcastUiState())

    private fun initUiState() {
        viewModelScope.launch {
            _uiState.update { podcastRepository.getPodcastModel(id) }
        }
    }

}

sealed class PodcastState {
    data object Loading : PodcastState()
    data object Success : PodcastState()
    data class Failure(val errorMessage: String?) : PodcastState()
}

data class PodcastUiState(
    val state: PodcastState = PodcastState.Loading,
    val author: String = "",
    val title: String = "",
    val cover: String = "",
    val description: String = "",
    val episodes: List<Episode> = emptyList()
)