package dev.halim.shelfdroid.expect


import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual class MediaManager actual constructor(val player: PlatformPlayer) {
    private var seekTime = 0L

    private val _playerState = MutableStateFlow(getMediaPlayerStateFromExoPlayer())
    actual val playerState = _playerState.asStateFlow()

    init {
        setupPlayerListeners()
        startProgressTracking()
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
                } else PlaybackState.Pause
            }

            Player.STATE_IDLE -> PlaybackState.Idle
            Player.STATE_ENDED -> PlaybackState.Ended
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
                updatePlayerState(isPlaying, player.playbackState)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerState(player.isPlaying, playbackState)
            }
        })
    }

    private fun startProgressTracking() {
//        coroutineScope.launch {
//            while (true) {
//                delay(1000)
//                if (platformPlayer.isPlaying) {
//                    _playerState.update {
//                        it.copy(progress = platformPlayer.currentPosition)
//                    }
//                }
//            }
//        }
    }

    actual fun playBookUiState(uiState: BookUiState) {
        if (_playerState.value.item?.id != uiState.id) {
            player.pause()
            player.clearMediaItems()
            val mediaItem = MediaItem.Builder()
                .setUri(uiState.url)
                .setMediaId(uiState.id)
                .build()

            player.addMediaItem(mediaItem)
            seekTime = uiState.seekTime
            player.prepare()
            player.play()
        } else {
            if (_playerState.value.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
}

actual class PlatformMediaItem(mediaItem: MediaItem) {
    actual val id: String = mediaItem.mediaId
}

actual typealias PlatformPlayer = Player