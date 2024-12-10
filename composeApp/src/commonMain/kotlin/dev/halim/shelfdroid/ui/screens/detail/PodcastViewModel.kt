package dev.halim.shelfdroid.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PodcastViewModel(private val id: String) : ViewModel() {
    private val _uiState = MutableStateFlow(PodcastUiState())
    val uiState: StateFlow<PodcastUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PodcastUiState())
}

sealed class PodcastState {
    data object Loading : PodcastState()
    data object Success : PodcastState()
    data class Failure(val errorMessage: String?) : PodcastState()
}

data class PodcastUiState(
    val state: PodcastState = PodcastState.Loading,
)