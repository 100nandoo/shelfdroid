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
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

actual class MediaManager actual constructor(
    private val player: PlatformPlayer,
    private val dataStoreManager: DataStoreManager,
    private val sessionManager: SessionManager,
    private val timer: Timer,
) {
    private val _playerState = MutableStateFlow(
        MediaPlayerState(
            isPlaying = player.isPlaying,
            playbackState = mapPlayerState(player.isPlaying, player.playbackState),
            item = player.currentMediaItem?.let { PlatformMediaItem(it) },
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
    }

    actual fun playBookUiState(item: ShelfdroidMediaItemImpl) {
        if (_playerState.value.item?.id != item.id) {
            pause()
            changeItem(item)
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

    actual fun changeChapter(item: ShelfdroidMediaItemImpl) {
        pause()
        changeItem(item)
        play()
        dataStoreEventChangeMediaItem(item)
    }

    actual fun play() {
        player.play()
        timerEventStartTimer()
        sessionEventPlay()
    }

    actual fun pause() {
        player.pause()
        val timeListened = timerEventStop()
        sessionEventPause(timeListened)
        dataStoreEventUpdateCurrentPosition()
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

    actual fun setSleepTimer(duration: Duration){
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
                PlaybackState.Ended
            }

            else -> _playerState.value.playbackState
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

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _playerState
                    .update { it.copy(item = mediaItem?.let { PlatformMediaItem(it) }) }
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
        _playerState.value.item?.id?.let { itemId -> sessionManager.onEvent(SessionEvent.Play(itemId)) }
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

    private fun dataStoreEventChangeMediaItem(shelfdroidMediaItem: ShelfdroidMediaItemImpl) {
        dataStoreManager.onEvent(DataStoreEvent.MediaItemChanged(shelfdroidMediaItem))
    }

    private fun dataStoreEventUpdateCurrentPosition() {
        val currentPosition = player.currentPosition
        dataStoreManager.onEvent(DataStoreEvent.UpdateCurrentPosition(currentPosition))
    }

    private fun changeItem(item: ShelfdroidMediaItemImpl) {
        val mediaItem = item.toMediaItem()
        player.setMediaItem(mediaItem)
        player.prepare()
    }
}

fun ShelfdroidMediaItemImpl.toMediaItem(): MediaItem {
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

actual class PlatformMediaItem(mediaItem: MediaItem) {
    actual val id: String = mediaItem.mediaId
}

actual typealias PlatformPlayer = Player
