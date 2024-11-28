package dev.halim.shelfdroid.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.repo.PlayerRepository
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

class PlayerViewModel(
    private val playerRepository: PlayerRepository, private val mediaManager: MediaManager, val id: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState
        .onStart { initUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerUiState())

    private val _playerProgressUiState = MutableStateFlow(PlayerProgressUiState())
    val playerProgressUiState = _playerProgressUiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerProgressUiState())

    private val _advanceUiState = MutableStateFlow(AdvanceUiState())
    val advanceUiState = _advanceUiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdvanceUiState())

    val playerState = mediaManager.playerState
        .stateIn(viewModelScope, SharingStarted.Lazily, MediaPlayerState())

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.ProgressChanged -> {
                val result = initProgress(event.progress)
                _playerProgressUiState.update {
                    result
                }
                val positionMs = (result.currentTime * 1000).toLong()
                mediaManager.seekTo(positionMs)
            }

            is PlayerEvent.AddBookmark -> TODO()
            is PlayerEvent.SetSleepTimer -> {
                mediaManager.setSleepTimer(event.duration)
            }
            is PlayerEvent.ChangeSpeed -> {
                _advanceUiState.update { it.copy(speed = event.speed) }
                mediaManager.changeSpeed(event.speed)
            }
            is PlayerEvent.VolumeChanged -> TODO()
            PlayerEvent.SeekBack, PlayerEvent.SeekForward -> {
                handleSeekEvent(event)
            }
            is PlayerEvent.PlayBook -> {
                mediaManager.playBookUiState(_uiState.value.bookPlayerUiState.toImpl())
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
            _playerProgressUiState.update {
                initProgress()
            }

            mediaManager.playerState.collect {
                if (it.item?.id == id) {
                    it.currentPosition.collect { position ->
                        _playerProgressUiState.emit(initProgress(position))
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

    private fun handleSeekEvent(event: PlayerEvent) {
        val delta = if (event is PlayerEvent.SeekBack) {
            mediaManager.seekBackward()
            -10
        } else {
            mediaManager.seekForward()
            10
        }
        _playerProgressUiState.update {
            val currentTime = (it.currentTime + delta).coerceIn(0.0, it.totalTime)
            val progress = (currentTime / it.totalTime).toFloat()
            it.copy(currentTime = currentTime, progress = progress)
        }

    }
}

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

data class PlayerProgressUiState(
    val currentTime: Double = 0.0,
    val totalTime: Double = 0.0,
    val progress: Float = 0f
)

data class AdvanceUiState(
    val speed: Float = 1f,
    val timer: Long = 0
)

data class PlayerUiState(
    val state: PlayerState = PlayerState.Loading,
    val bookPlayerUiState: BookPlayerUiState = BookPlayerUiState()
)

sealed class PlayerState {
    data object Loading : PlayerState()
    data object Success : PlayerState()
    data class Failure(val errorMessage: String?) : PlayerState()
}

sealed class PlayerEvent {
    data object SeekBack : PlayerEvent()
    data object SeekForward : PlayerEvent()
    data class ChangeSpeed(val speed: Float) : PlayerEvent()
    data class SetSleepTimer(val duration: Duration) : PlayerEvent()
    data class VolumeChanged(val volume: Float) : PlayerEvent()
    data class AddBookmark(val currentTime: Double, val notes: String) : PlayerEvent()
    data class ProgressChanged(val progress: Float) : PlayerEvent()
    data object PlayBook : PlayerEvent()
}