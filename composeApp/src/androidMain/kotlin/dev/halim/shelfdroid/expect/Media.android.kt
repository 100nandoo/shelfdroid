package dev.halim.shelfdroid.expect


import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.SyncSessionRequest
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
    private val api: Api
) {
    private var seekTime = 0L
    private var startTime = 0L
    private var duration = 0L
    private var sessionId: String = ""

    private val _playerState = MutableStateFlow(getMediaPlayerStateFromExoPlayer())
    actual val playerState = _playerState.asStateFlow()

    init {
        setupPlayerListeners()
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
                if (seekTime > 0) {
                    player.seekTo(seekTime)
                    seekTime = 0
                }
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

    private fun startSession(itemId: String) {
        io.launch {
            api.playBook(itemId).collect { result ->
                result.onSuccess { response ->
                    sessionId = response.id
                }
            }

        }
    }

    private fun syncSession() {
        val current = player.currentPosition / 1000
        io.launch {
            val time = current - startTime / 1000
            val request = SyncSessionRequest(current, time, duration)
            api.syncSession(sessionId, request).collect { response ->
                response.isSuccess
            }
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
                updatePlayerState(isPlaying, player.playbackState)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerState(player.isPlaying, playbackState)
            }
        })
    }

    actual fun playBookUiState(uiState: BookUiState) {
        if (_playerState.value.item?.id != uiState.id) {
            player.pause()
            player.clearMediaItems()

            val mediaItem = uiState.toMediaItem()
            player.addMediaItem(mediaItem)
            seekTime = uiState.seekTime
            startTime = uiState.seekTime
            duration = uiState.duration.toLong()

            player.prepare()
            player.play()

            updateCurrentItem(uiState)
            startSession(uiState.id)
        } else {
            if (_playerState.value.isPlaying) {
                player.pause()
                updateCurrentPosition()
                syncSession()
            } else {
                player.play()
            }
        }
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
        .build()
    return mediaItem
}

actual class PlatformMediaItem(mediaItem: MediaItem) {
    actual val id: String = mediaItem.mediaId
}

actual typealias PlatformPlayer = Player