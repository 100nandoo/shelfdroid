package dev.halim.shelfdroid.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.store.ItemExtensions.toBookUiState
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.StoreOutput
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.impl.extensions.get

class PlayerViewModel(
    private val storeManager: StoreManager, private val mediaManager: MediaManager, val id: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState
        .onStart { initUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

    fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.ProgressChanged -> TODO()
        }
    }

    private fun initUiState() {
        viewModelScope.launch {
            val result = storeManager.itemStore.get(ItemKey.Single(id)) as StoreOutput.Single
            _uiState.update { it.copy(state = PlayerState.Success, bookUiState = result.data.toBookUiState()) }
        }
    }
}

data class PlayerUiState(val state: PlayerState = PlayerState.Loading, val bookUiState: BookUiState = BookUiState())

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
    data object ProgressChanged : PlayerEvent()
}