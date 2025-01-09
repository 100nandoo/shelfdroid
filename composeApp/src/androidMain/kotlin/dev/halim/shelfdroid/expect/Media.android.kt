package dev.halim.shelfdroid.expect


import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dev.halim.shelfdroid.datastore.DataStoreEvent
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.player.SessionEvent
import dev.halim.shelfdroid.player.SessionManager
import dev.halim.shelfdroid.player.Timer
import dev.halim.shelfdroid.player.TimerEvent
import dev.halim.shelfdroid.player.TimerEventReturn
import dev.halim.shelfdroid.ui.MediaItemBook
import dev.halim.shelfdroid.ui.MediaItemPodcast
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration

actual class MediaManager actual constructor(
    private val player: PlatformPlayer,
    private val dataStoreManager: DataStoreManager,
    private val sessionManager: SessionManager,
    private val timer: Timer,
    private val main: CoroutineScope
) {
    private val _playerState = MutableStateFlow(
        MediaPlayerState(
            isPlaying = player.isPlaying,
            playbackState = mapPlayerState(player.isPlaying, player.playbackState),
            book = null,
        )
    )
    actual val playerState = _playerState.asStateFlow()

    actual val currentPosition: Flow<Long> = flow {
        while (player.isPlaying) {
            delay(1000)
            emit(player.currentPosition)
        }
    }

    private val seekIncrement = 10_000

    init {
        setupPlayerListeners()
        playerStatePlayingListener()
    }

    actual fun playBook(item: MediaItemBook) {
        if (_playerState.value.itemId != item.id) {
            pause()
            changeBook(item)
            play()
            seekTo(item.seekTime)
            dataStoreEventChangeMediaItem(item)
        } else {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    actual fun changeChapter(item: MediaItemBook) {
        val timeListened = timerEventStop()
        sessionEventPause(timeListened)
        dataStoreEventUpdateCurrentPosition()

        pause()
        changeBook(item)
        play()
        seekTo(item.seekTime)
        dataStoreEventChangeMediaItem(item)
    }

    actual fun playPodcast(item: MediaItemPodcast) {
        if (_playerState.value.itemId != item.id) {
            pause()
            changePodcast(item)
            play()
            seekTo(item.seekTime)
            dataStoreEventChangeMediaItem(item)
        } else {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                play()
            }
        }
    }

    actual fun play() {
        player.play()
    }

    actual fun pause() {
        player.pause()
    }

    actual fun seekForward() {
        seekTo(player.currentPosition + seekIncrement)
    }

    actual fun seekBackward() {
        seekTo(player.currentPosition - seekIncrement)
    }

    actual fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    actual fun changeSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    actual fun setSleepTimer(duration: Duration) {
        timerEventStartSleepTimer(duration)
    }

    private fun mapPlayerState(isPlaying: Boolean, playbackState: Int): PlaybackState {
        return when (playbackState) {
            Player.STATE_BUFFERING -> PlaybackState.Buffering
            Player.STATE_READY -> {
                if (isPlaying) {
                    PlaybackState.Playing
                } else {
                    PlaybackState.Pause
                }
            }

            Player.STATE_IDLE -> {
                PlaybackState.Idle
            }

            Player.STATE_ENDED -> {
                playerStateEndedListener()
                PlaybackState.Ended
            }

            else -> _playerState.value.playbackState
        }
    }

    private fun playerStatePlayingListener() {
        main.launch {
            _playerState.collect { state ->
                if (state.playbackState == PlaybackState.Playing) {
                    timerEventStartTimer()
                    sessionEventPlay()
                } else if (state.playbackState == PlaybackState.Pause || state.playbackState == PlaybackState.Ended) {
                    val timeListened = timerEventStop()
                    sessionEventPause(timeListened)
                    dataStoreEventUpdateCurrentPosition()
                }
            }
        }
    }

    private fun playerStateEndedListener() {
        val currentItem = _playerState.value.book
        if (currentItem?.chapters.isNullOrEmpty()) return
        currentItem?.let { item ->
            val currentChapterIndex = item.chapters.indexOfFirst { item.currentChapter?.id == it.id }
            val newChapterIndex = currentChapterIndex + 1
            if (currentChapterIndex > 0 && newChapterIndex in item.chapters.indices) {
                changeChapter(MediaItemBook(item, newChapterIndex))
            }
        }
    }

    private fun setupPlayerListeners() {
        fun updatePlayerState(isPlaying: Boolean, playbackState: Int) {
            val targetState = mapPlayerState(isPlaying, playbackState)
            _playerState.update { it.copy(isPlaying = isPlaying, playbackState = targetState) }
        }

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                println("player error: ${error.errorCodeName}")
                super.onPlayerError(error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("Media", "isPlaying: $isPlaying")
                updatePlayerState(isPlaying, player.playbackState)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("Media", "onPlaybackStateChanged: $playbackState")
                updatePlayerState(player.isPlaying, playbackState)
            }
        })
    }

    private fun sessionEventPlay() {
        if (_playerState.value.podcast == null) {
            _playerState.value.itemId?.let { itemId -> sessionManager.onEvent(SessionEvent.PlayBook(itemId)) }
        } else {
            _playerState.value.itemId?.let { itemId ->
                _playerState.value.podcast?.let { podcast ->
                    sessionManager.onEvent(SessionEvent.PlayPodcast(podcast.libraryItemId, itemId))
                }
            }
        }
    }

    private fun sessionEventPause(timeListened: Long) {
        val time = (player.currentPosition + (player.currentMediaItem
            ?.clippingConfiguration?.startPositionMs ?: 0L)) / 1000
        sessionManager.onEvent(SessionEvent.Pause(time, timeListened))
    }

    private fun timerEventStartTimer() {
        timer.onEvent(TimerEvent.StartTimeListened)
    }

    private fun timerEventStop(): Long {
        return timer.onEventReturned(TimerEventReturn.StopTimeListened)
    }

    private fun timerEventStartSleepTimer(duration: Duration) {
        timer.onEvent(
            TimerEvent.StartSleepTimer(
                duration,
                { timeLeft -> _playerState.update { it.copy(sleepTimeLeft = timeLeft) } },
                { pause() })
        )
    }

    private fun dataStoreEventChangeMediaItem(shelfdroidMediaItem: ShelfdroidMediaItem) {
        dataStoreManager.onEvent(DataStoreEvent.MediaItemChanged(shelfdroidMediaItem))
    }

    private fun dataStoreEventUpdateCurrentPosition() {
        val currentPosition = player.currentPosition
        dataStoreManager.onEvent(DataStoreEvent.UpdateCurrentPosition(currentPosition))
    }

    private fun changeBook(item: MediaItemBook) {
        val mediaItem = item.toMediaItem()
        _playerState.update { it.copy(book = item, itemId = item.id, podcast = null) }
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    private fun changePodcast(item: MediaItemPodcast) {
        val mediaItem = item.toMediaItem()
        _playerState.update { it.copy(podcast = item, itemId = item.id, book = null) }
        player.setMediaItem(mediaItem)
        player.prepare()
    }
}

fun MediaItemPodcast.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(this.title)
        .setArtist(this.author)
        .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
        .build()
    val mediaItem = MediaItem.Builder()
        .setUri(this.url)
        .setMediaId(this.id)
        .setMediaMetadata(metadata)
        .build()
    return mediaItem
}

fun MediaItemBook.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(this.title)
        .setArtist(this.author)
        .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
        .build()
    val mediaItem = MediaItem.Builder()
        .setUri(this.url)
        .setMediaId(this.id)
        .setMediaMetadata(metadata)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startTime)
                .setEndPositionMs(endTime)
                .build()
        )
        .build()
    return mediaItem
}

actual typealias PlatformPlayer = Player
