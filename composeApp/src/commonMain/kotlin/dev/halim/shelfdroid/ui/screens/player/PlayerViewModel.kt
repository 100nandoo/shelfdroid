package dev.halim.shelfdroid.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playerRepository: PlayerRepository, private val mediaManager: MediaManager, val id: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState
        .onStart { initUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

    private val _playerProgress = MutableStateFlow(PlayerProgressUiState())
    val playerProgressUiState = _playerProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerProgressUiState())

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.ProgressChanged -> {
                val result = initProgress(event.progress)
                _playerProgress.update {
                    result
                }
               val positionMs = (result.currentTime * 1000).toLong()
                mediaManager.seekTo(positionMs)
            }
        }
    }

    private fun initUiState() {
        viewModelScope.launch {
            val result = playerRepository.getPlayerModel(id)
            _uiState.update {
                it.copy(
                    state = PlayerState.Success,
                    bookPlayerUiState = result
                )
            }
            _playerProgress.update {
                initProgress()
            }

            mediaManager.playerState.collect {
                if (it.item?.id == id) {
                    mediaManager.playerState.value.currentPosition.collect { position ->
                        _playerProgress.emit(initProgress(position))
                    }
                }
            }
        }
    }

    private fun initProgress(progress: Float): PlayerProgressUiState {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentChapter = bookPlayerUiState.currentChapter
        val totalTime = currentChapter.end - currentChapter.start
        val currentTime = progress * totalTime
        return PlayerProgressUiState(currentTime, totalTime, progress)
    }


    private fun initProgress(position: Long = 0): PlayerProgressUiState {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentChapter = bookPlayerUiState.currentChapter
        val currentTime = if (position == 0L) bookPlayerUiState.seekTime / 1000 else position / 1000
        val totalTime = currentChapter.end - currentChapter.start
        val progress = (currentTime / totalTime).toFloat()
        return PlayerProgressUiState(currentTime.toDouble(), totalTime, progress)
    }
}

data class PlayerProgressUiState(
    val currentTime: Double = 0.0,
    val totalTime: Double = 0.0,
    val progress: Float = 0f
)

class BookPlayerUiState(
    override val id: String = "",
    override val author: String = "",
    override val title: String = "",
    override val cover: String = "",
    override val url: String = "",
    override val seekTime: Long = 0L,
    override val startTime: Long = 0L,
    override val endTime: Long = 0L,
    val progress: Float = 0f,
    val chapters: List<BookChapter> = emptyList(),
    val currentChapter: BookChapter = BookChapter(),
) : ShelfdroidMediaItem() {
    override fun toImpl(): ShelfdroidMediaItemImpl = ShelfdroidMediaItemImpl(
        id, author, title, cover, url, seekTime,
        startTime, endTime
    )
}

data class PlayerUiState(
    val state: PlayerState = PlayerState.Loading,
    val bookPlayerUiState: BookPlayerUiState = BookPlayerUiState()
)

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
    data class ProgressChanged(val progress: Float) : PlayerEvent()
}