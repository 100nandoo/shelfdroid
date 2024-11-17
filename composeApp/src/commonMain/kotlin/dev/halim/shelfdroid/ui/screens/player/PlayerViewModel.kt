package dev.halim.shelfdroid.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.network.Api
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(private val api: Api, private val mediaManager: MediaManager) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.SeekBack -> {
                mediaManager.seekBackward()
            }

            PlayerEvent.SeekForward -> {
                mediaManager.seekForward()
            }

            PlayerEvent.PlayPause -> {
                TODO()
            }

            PlayerEvent.ProgressChanged -> TODO()
        }
    }
}

data class PlayerUiState(val state: PlayerState = PlayerState.Loading)

sealed class PlayerState {
    data object Loading : PlayerState()
    data object Success : PlayerState()
    data class Failure(val errorMessage: String?) : PlayerState()
}

sealed class AdvancedPlayerEvent {
    data class SpeedChanged(val speed: Float) : AdvancedPlayerEvent()
    data class SleepTimerSet(val minute: Int) : AdvancedPlayerEvent()
    data class VolumeChanged(val volume: Float) : AdvancedPlayerEvent()
    data class AddBookmark(val currentTime: Double, val notes: String) : AdvancedPlayerEvent()
}

sealed class PlayerEvent {
    data object SeekBack : PlayerEvent()
    data object SeekForward : PlayerEvent()
    data object PlayPause : PlayerEvent()
    data object ProgressChanged : PlayerEvent()
}