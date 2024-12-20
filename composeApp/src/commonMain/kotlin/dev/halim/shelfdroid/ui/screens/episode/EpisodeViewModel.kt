package dev.halim.shelfdroid.ui.screens.episode

import androidx.compose.ui.text.LinkAnnotation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.repo.EpisodeRepository
import dev.halim.shelfdroid.ui.generic.GenericState
import dev.halim.shelfdroid.ui.screens.podcast.EpisodeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EpisodeViewModel(
    private val episodeRepository: EpisodeRepository,
    private val id: String,
    private val episodeId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(EpisodeScreenUiState())
    val uiState: StateFlow<EpisodeScreenUiState> = _uiState
        .onStart { initUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EpisodeScreenUiState())

    private fun initUiState() {
        viewModelScope.launch {
            _uiState.update { episodeRepository.getEpisodeModel(id, episodeId) }
        }
    }

    fun onEvent(event: EpisodeEvent) {
    }

}

data class EpisodeScreenUiState(
    val state: GenericState = GenericState.Loading,
    val episodeUiState: EpisodeUiState = EpisodeUiState(),
)

sealed class EpisodeEvent {
}