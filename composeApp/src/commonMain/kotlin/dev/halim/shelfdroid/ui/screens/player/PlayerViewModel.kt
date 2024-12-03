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
            is PlayerEvent.ProgressChangedFinish -> {
                val result = updateCurrentTime(event.progress)
                val positionMs = (result.currentTime * 1000).toLong()
                mediaManager.seekTo(positionMs)
                mediaManager.playBookUiState(_uiState.value.bookPlayerUiState.toImpl())
            }
            is PlayerEvent.ProgressChanged -> {
                mediaManager.pause()
                _playerProgressUiState.update { updateCurrentTime(event.progress) }
            }
            PlayerEvent.PreviousChapter -> {
                changeChapter(true)
            }
            PlayerEvent.NextChapter -> {
                changeChapter(false)
            }
            is PlayerEvent.AddBookmark -> TODO()
            is PlayerEvent.SetSleepTimer -> {
                viewModelScope.launch {
                    playerState.collect { playerState ->
                        _advanceUiState.update { it.copy(sleepTimeLeft = playerState.sleepTimeLeft) }
                    }
                }
                mediaManager.setSleepTimer(event.duration)
                _advanceUiState.update { it.copy(sleepTimeLeft = event.duration) }
            }

            is PlayerEvent.ChangeSpeed -> {
                _advanceUiState.update { it.copy(speed = event.speed) }
                mediaManager.changeSpeed(event.speed)
            }

            is PlayerEvent.VolumeChanged -> TODO()
            PlayerEvent.SeekBack -> {
                mediaManager.seekBackward()
            }

            PlayerEvent.SeekForward -> {
                mediaManager.seekForward()
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
                initProgressUiState()
            }
            mediaManager.playerState.collect {
                if (it.item?.id == id) {
                    mediaManager.currentPosition.collect { position ->
                        _playerProgressUiState.emit(initProgressUiState(position))
                    }
                }
            }
        }
    }

    private fun updateCurrentTime(progress: Float): PlayerProgressUiState {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentChapter = bookPlayerUiState.currentChapter
        val totalTime = currentChapter.end - currentChapter.start
        val currentTime = progress * totalTime
        return PlayerProgressUiState(currentTime, totalTime, progress)
    }

    private fun initProgressUiState(position: Long = 0): PlayerProgressUiState {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentChapter = bookPlayerUiState.currentChapter
        val currentTime = if (position == 0L) bookPlayerUiState.seekTime / 1000 else position / 1000
        val totalTime = currentChapter.end - currentChapter.start
        val progress = (currentTime / totalTime).toFloat()
        return PlayerProgressUiState(currentTime.toDouble(), totalTime, progress)
    }

    private fun changeChapter(isPrevious: Boolean) {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentIndex = bookPlayerUiState.chapters.indexOfFirst { it.id == bookPlayerUiState.currentChapter.id }
        val newIndex = currentIndex + if (isPrevious) -1 else 1

        if (newIndex in bookPlayerUiState.chapters.indices) {
            val updatedChapter = bookPlayerUiState.chapters[newIndex]
            val updatedBookPlayerUiState = BookPlayerUiState(bookPlayerUiState, updatedChapter)
            mediaManager.changeChapter(updatedBookPlayerUiState.toImpl())
            _uiState.update {
                it.copy(bookPlayerUiState = updatedBookPlayerUiState)
            }
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

    constructor(
        existing: BookPlayerUiState,
        currentChapter: BookChapter = existing.currentChapter,
    ) : this(
        id = existing.id,
        author = existing.author,
        title = existing.title,
        cover = existing.cover,
        url = existing.url,
        seekTime = 0,
        startTime = currentChapter.start.toLong() * 1000,
        endTime = currentChapter.end.toLong() * 1000,
        progress = existing.progress,
        chapters = existing.chapters,
        currentChapter = currentChapter
    )
}

data class PlayerProgressUiState(
    val currentTime: Double = 0.0,
    val totalTime: Double = 0.0,
    val progress: Float = 0f
)

data class AdvanceUiState(
    val speed: Float = 1f,
    val sleepTimeLeft: Duration = Duration.ZERO
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
    data object PreviousChapter : PlayerEvent()
    data object NextChapter : PlayerEvent()
    data class ChangeSpeed(val speed: Float) : PlayerEvent()
    data class SetSleepTimer(val duration: Duration) : PlayerEvent()
    data class VolumeChanged(val volume: Float) : PlayerEvent()
    data class AddBookmark(val currentTime: Double, val notes: String) : PlayerEvent()
    data class ProgressChangedFinish(val progress: Float) : PlayerEvent()
    data class ProgressChanged(val progress: Float) : PlayerEvent()
    data object PlayBook : PlayerEvent()
}