package dev.halim.shelfdroid.expect


import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.halim.shelfdroid.SharedObject
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual class MediaManager actual constructor(playerWrapper: PlayerWrapper) {
    private var exoPlayer: MediaController = playerWrapper.mediaController
    private var seekTime = 0L

    private val _playerState = MutableStateFlow(getMediaPlayerStateFromExoPlayer())
    actual val playerState = _playerState.asStateFlow()

    init {
        setupPlayerListeners()
        startProgressTracking()
    }

    private fun getMediaPlayerStateFromExoPlayer(): MediaPlayerState {
        return MediaPlayerState(
            isPlaying = exoPlayer.isPlaying,
            playbackState = mapPlayerState(exoPlayer.isPlaying, exoPlayer.playbackState),
            currentMediaItemWrapper = exoPlayer.currentMediaItem?.let { MediaItemWrapper(it) }
        )
    }

    private fun mapPlayerState(isPlaying: Boolean, playbackState: Int): PlaybackState {
        return when (playbackState) {
            Player.STATE_BUFFERING -> PlaybackState.Buffering
            Player.STATE_READY -> {
                if (seekTime > 0) {
                    exoPlayer.seekTo(seekTime)
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

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _playerState
                    .update { it.copy(currentMediaItemWrapper = mediaItem?.let { MediaItemWrapper(it) }) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerState(isPlaying, exoPlayer.playbackState)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerState(exoPlayer.isPlaying, playbackState)
            }
        })
    }

    private fun restart(){
        SharedObject.playerWrapper?.mediaController?.let {
            exoPlayer = it
            _playerState.value = getMediaPlayerStateFromExoPlayer()
            setupPlayerListeners()
        }
    }

    private fun startProgressTracking() {
//        coroutineScope.launch {
//            while (true) {
//                delay(1000)
//                if (exoPlayer.isPlaying) {
//                    _playerState.update {
//                        it.copy(progress = exoPlayer.currentPosition)
//                    }
//                }
//            }
//        }
    }

    actual fun playBookUiState(uiState: BookUiState) {
        if (exoPlayer.isConnected.not()) {
            restart()
        }
        if (_playerState.value.currentMediaItemWrapper?.mediaId != uiState.id) {
            exoPlayer.pause()
            exoPlayer.clearMediaItems()
            val mediaItem = MediaItem.Builder()
                .setUri(uiState.url)
                .setMediaId(uiState.id)
                .build()

            exoPlayer.addMediaItem(mediaItem)
            seekTime = uiState.seekTime
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            if (_playerState.value.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }
    }

    actual fun release() {
        exoPlayer.release()
    }
}

actual class MediaItemWrapper(mediaItem: MediaItem) {
    actual val mediaId: String = mediaItem.mediaId
}

actual class PlayerWrapper(val mediaController: MediaController)