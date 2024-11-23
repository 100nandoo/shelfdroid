package dev.halim.shelfdroid.expect


import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

actual class MediaManager actual constructor(
    private val player: PlatformPlayer,
    private val dataStoreManager: DataStoreManager,
    private val io: CoroutineScope,
    private val main: CoroutineScope,
    private val sessionManager: SessionManager
) {
    private val _playerState = MutableStateFlow(getMediaPlayerStateFromExoPlayer())
    actual val playerState = _playerState.asStateFlow()

    init {
        setupPlayerListeners()
        observePlayerState()
    }

    private fun observePlayerState() {
        io.launch {
            playerState.collect { state ->
                main.launch {
                    if (state.playbackState is PlaybackState.Pause) {
                        val time = player.currentPosition / 1000
                        sessionManager.onEvent(SessionEvent.Pause(time))
                    }
                }
            }
        }
    }

    private fun getMediaPlayerStateFromExoPlayer(): MediaPlayerState {
        return MediaPlayerState(
            isPlaying = player.isPlaying,
            playbackState = mapPlayerState(player.isPlaying, player.playbackState),
            item = player.currentMediaItem?.let { PlatformMediaItem(it) }
        )
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

    private fun updateCurrentPosition() {
        val currentPosition = player.currentPosition
        io.launch {
            dataStoreManager.setCurrentPosition(currentPosition)
        }
    }

    private fun updateCurrentItem(uiState: BookUiState) {
        io.launch {
            dataStoreManager.writeSerializable(::BookUiState.name, uiState, BookUiState.serializer())
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

    actual fun playBookUiState(uiState: BookUiState) {
        if (_playerState.value.item?.id != uiState.id) {
            player.pause()
            val mediaItem = uiState.toMediaItem()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            sessionManager.onEvent(SessionEvent.Play(uiState))
            updateCurrentItem(uiState)
        } else {
            if (_playerState.value.isPlaying) {
                player.pause()
                updateCurrentPosition()
            } else {
                player.play()
            }
        }
    }

    actual fun seekForward() {
        player.seekForward()
    }

    actual fun seekBackward() {
        player.seekBack()
    }

    actual fun changeSpeed(speed: Float){
        player.setPlaybackSpeed(speed)
    }
}

fun BookUiState.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(this.title)
        .setArtist(this.author)
        .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK)
        .build()
    val mediaItem = MediaItem.Builder()
        .setUri(this.url)
        .setMediaId(this.id)
        .setMediaMetadata(metadata)
        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setStartPositionMs(seekTime).build())
        .build()
    return mediaItem
}

actual class PlatformMediaItem(mediaItem: MediaItem) {
    actual val id: String = mediaItem.mediaId
}

actual typealias PlatformPlayer = Player
