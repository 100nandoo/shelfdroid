package dev.halim.shelfdroid.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.network.AudioBookmark
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.repo.PlayerRepository
import dev.halim.shelfdroid.ui.MediaItemType
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.MediaItemBook
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
                mediaManager.playBook(_uiState.value.bookPlayerUiState.toMediaItemBook())
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

            is PlayerEvent.JumpToChapter -> {
                jumpToChapter(event.target)
            }

            is PlayerEvent.JumpToBookmark -> {
                val bookmark = _uiState.value.bookPlayerUiState.bookmarks[event.index]
                val index = _uiState.value.bookPlayerUiState.chapters.indexOfFirst { chapter ->
                    bookmark.time >= chapter.start && bookmark.time <= chapter.end
                }
                val chapter = _uiState.value.bookPlayerUiState.chapters[index]
                val seekTime = (bookmark.time - chapter.start).toLong() * 1000
                if (index >= 0) {
                    jumpToChapter(index, seekTime)
                }
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
                mediaManager.playBook(_uiState.value.bookPlayerUiState.toMediaItemBook())
            }

        }
    }

    private fun collectFlows() {
        viewModelScope.launch {
            mediaManager.playerState.collect {
                if (it.itemId == id) {
                    mediaManager.currentPosition.collect { position ->
                        _playerProgressUiState.emit(initProgressUiState(position))
                    }
                }
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
            collectFlows()
        }
    }

    private fun updateCurrentTime(progress: Float): PlayerProgressUiState {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        return if (bookPlayerUiState.currentChapter != null){
            val currentChapter = bookPlayerUiState.currentChapter
            val totalTime = currentChapter.end - currentChapter.start
            val currentTime = progress * totalTime
             PlayerProgressUiState(currentTime, totalTime, progress)
        } else {
            val totalTime = (bookPlayerUiState.endTime / 1000).toDouble()
            val currentTime = progress * totalTime
            PlayerProgressUiState(currentTime, totalTime, progress)
        }
    }

    private fun initProgressUiState(position: Long = 0): PlayerProgressUiState {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentTime = if (position == 0L) bookPlayerUiState.seekTime / 1000 else position / 1000
        return if (bookPlayerUiState.currentChapter != null){
            val currentChapter = bookPlayerUiState.currentChapter
            val totalTime = currentChapter.end - currentChapter.start
            val progress = (currentTime / totalTime).toFloat()
             PlayerProgressUiState(currentTime.toDouble(), totalTime, progress)
        } else {
            val totalTime = (bookPlayerUiState.endTime / 1000).toDouble()
            val progress = (currentTime / totalTime).toFloat()
            PlayerProgressUiState(currentTime.toDouble(), totalTime, progress)
        }

    }

    private fun changeChapter(isPrevious: Boolean) {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        val currentIndex = bookPlayerUiState.chapters.indexOfFirst { it.id == bookPlayerUiState.currentChapter?.id }
        val newIndex = currentIndex + if (isPrevious) -1 else 1

        if (newIndex in bookPlayerUiState.chapters.indices) {
            val updatedChapter = bookPlayerUiState.chapters[newIndex]
            val updatedBookPlayerUiState = BookPlayerUiState(bookPlayerUiState, updatedChapter)
            mediaManager.changeChapter(updatedBookPlayerUiState.toMediaItemBook())
            _uiState.update {
                it.copy(bookPlayerUiState = updatedBookPlayerUiState)
            }
        }
    }

    private fun jumpToChapter(target: Int, seekTime: Long = 0) {
        val bookPlayerUiState = _uiState.value.bookPlayerUiState
        if (target in bookPlayerUiState.chapters.indices) {
            val targetChapter = bookPlayerUiState.chapters[target]
            val updatedBookPlayerUiState = BookPlayerUiState(bookPlayerUiState, targetChapter, seekTime)
            mediaManager.changeChapter(updatedBookPlayerUiState.toMediaItemBook())
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
    override val type: MediaItemType = MediaItemType.Book,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val currentChapter: BookChapter? = BookChapter(),
    val chapters: List<BookChapter> = emptyList(),
    val progress: Float = 0f,
    val bookmarks: List<AudioBookmark> = emptyList(),
) : ShelfdroidMediaItem() {

    fun toMediaItemBook(): MediaItemBook {
        return MediaItemBook(
            id = id,
            author = author,
            title = title,
            cover = cover,
            url = url,
            seekTime = seekTime,
            type = type,
            startTime = startTime,
            endTime = endTime,
            currentChapter = currentChapter,
            chapters = chapters,
        )
    }

    constructor(
        existing: BookPlayerUiState,
        currentChapter: BookChapter? = existing.currentChapter,
        seekTime: Long = 0
    ) : this(
        id = existing.id,
        author = existing.author,
        title = existing.title,
        cover = existing.cover,
        url = existing.url,
        seekTime = seekTime,
        startTime = currentChapter?.start?.toLong()?.times(1000) ?: 0,
        endTime = currentChapter?.end?.toLong()?.times(1000) ?: 0,
        currentChapter = currentChapter,
        chapters = existing.chapters,
        progress = existing.progress,
        bookmarks = existing.bookmarks
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
    data class JumpToChapter(val target: Int) : PlayerEvent()
    data class ChangeSpeed(val speed: Float) : PlayerEvent()
    data class SetSleepTimer(val duration: Duration) : PlayerEvent()
    data class VolumeChanged(val volume: Float) : PlayerEvent()
    data class JumpToBookmark(val index: Int) : PlayerEvent()
    data class AddBookmark(val currentTime: Double, val notes: String) : PlayerEvent()
    data class ProgressChangedFinish(val progress: Float) : PlayerEvent()
    data class ProgressChanged(val progress: Float) : PlayerEvent()
    data object PlayBook : PlayerEvent()
}