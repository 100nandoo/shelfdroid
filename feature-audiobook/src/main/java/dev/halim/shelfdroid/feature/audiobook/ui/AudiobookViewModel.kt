package dev.halim.shelfdroid.feature.audiobook.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.AudiobookRepository
import dev.halim.shelfdroid.feature.audiobook.ui.AudiobookUiState.Error
import dev.halim.shelfdroid.feature.audiobook.ui.AudiobookUiState.Loading
import dev.halim.shelfdroid.feature.audiobook.ui.AudiobookUiState.Success
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudiobookViewModel @Inject constructor(
    private val audiobookRepository: AudiobookRepository
) : ViewModel() {

    val uiState: StateFlow<AudiobookUiState> = audiobookRepository
        .audiobooks.map<List<String>, AudiobookUiState> { Success(data = it) }
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun addAudiobook(name: String) {
        viewModelScope.launch {
            audiobookRepository.add(name)
        }
    }
}

sealed interface AudiobookUiState {
    object Loading : AudiobookUiState
    data class Error(val throwable: Throwable) : AudiobookUiState
    data class Success(val data: List<String>) : AudiobookUiState
}
