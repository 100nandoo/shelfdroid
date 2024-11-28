package dev.halim.shelfdroid.expect


import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import dev.halim.shelfdroid.datastore.DataStoreEvent
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlin.time.DurationUnit
import kotlin.time.toDuration

actual class MediaManager actual constructor(
    private val player: PlatformPlayer,
    private val dataStoreManager: DataStoreManager,
    private val sessionManager: SessionManager
) {
    private val _playerState = MutableStateFlow(setupPlayerState())
    actual val playerState = _playerState.asStateFlow()

    init {
        setupPlayerListeners()
    }

    actual fun playBookUiState(item: ShelfdroidMediaItemImpl) {
        if (_playerState.value.item?.id != item.id) {
            pause()
            changeItem(item)
            player.play()
            player.seekTo(item.seekTime)
            sessionEventPlay(item)
            dataStoreEventChangeMediaItem(item)
        } else {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                player.play()
            }
        }
    }

    actual fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    actual fun seekForward() {
        player.seekForward()
    }

    actual fun seekBackward() {
        player.seekBack()
    }

    actual fun changeSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    private fun setupPlayerState(): MediaPlayerState {
        return MediaPlayerState(
            isPlaying = player.isPlaying,
            playbackState = mapPlayerState(player.isPlaying, player.playbackState),
            item = player.currentMediaItem?.let { PlatformMediaItem(it) },
            currentPosition = flow {
                while (currentCoroutineContext().isActive) {
                    if (player.currentPosition > 0 && player.isPlaying) {
                        println("currentPosition from flow: ${player.currentPosition}")
                        emit(player.currentPosition)
                    }
                    delay(1.toDuration(DurationUnit.SECONDS))
                }
            })
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

    private fun sessionEventPlay(shelfdroidMediaItem: ShelfdroidMediaItemImpl) {
        sessionManager.onEvent(SessionEvent.Play(shelfdroidMediaItem))
    }

    private fun sessionEventPause() {
        val time = (player.currentPosition + (player.currentMediaItem
            ?.clippingConfiguration?.startPositionMs ?: 0L)) / 1000
        sessionManager.onEvent(SessionEvent.Pause(time))
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

    private fun pause() {
        player.pause()
        sessionEventPause()
        dataStoreEventUpdateCurrentPosition()
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
