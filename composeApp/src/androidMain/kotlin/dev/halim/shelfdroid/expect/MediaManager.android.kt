package dev.halim.shelfdroid.expect


import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import dev.halim.shelfdroid.ContextUtils
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual class MediaManager {
    private var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession

    private val _playerState = MutableStateFlow(MediaPlayerState())
    actual val playerState = _playerState.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())


    init {
        val context = ContextUtils.context
        exoPlayer = ExoPlayer.Builder(context).build()
        mediaSession = MediaSession.Builder(context, exoPlayer).build()

        setupPlayerListeners()
        startProgressTracking()
    }

    private fun setupPlayerListeners() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _playerState
                    .update { it.copy(currentMediaItemWrapper = mediaItem?.let { MediaItemWrapper(it) }) }
            }

            private fun updatePlayerState(isPlaying: Boolean, playbackState: Int) {
                val targetState = when (playbackState) {
                    Player.STATE_BUFFERING -> PlaybackState.Buffering
                    Player.STATE_READY -> if (isPlaying) PlaybackState.Playing else PlaybackState.Pause
                    Player.STATE_IDLE -> PlaybackState.Idle
                    Player.STATE_ENDED -> PlaybackState.Ended
                    else -> _playerState.value.playbackState
                }
                _playerState.update { it.copy(isPlaying = isPlaying, playbackState = targetState) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayerState(isPlaying, exoPlayer.playbackState)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlayerState(exoPlayer.isPlaying, playbackState)
            }
        })
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
        if (_playerState.value.currentMediaItemWrapper?.mediaId != uiState.id) {
            exoPlayer.pause()
            exoPlayer.clearMediaItems()
            addItem(uiState)
            exoPlayer.play()
        } else {
            if (_playerState.value.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }
    }

    actual fun addItem(uiState: BookUiState) {
        val mediaItem = MediaItem.Builder().setUri(uiState.url).setMediaId(uiState.id).build()
        exoPlayer.addMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(uiState.seekTime)
    }

    actual fun currentItem(): MediaItemWrapper? {
        return exoPlayer.currentMediaItem?.let { MediaItemWrapper(it) }
    }

    actual fun play() {
        exoPlayer.play()
    }

    actual fun pause() {
        exoPlayer.pause()
    }

    actual fun release() {
        coroutineScope.cancel()
        exoPlayer.release()
        mediaSession.release()
    }

    actual fun isPlaying(): Boolean = exoPlayer.isPlaying
}

actual class MediaItemWrapper(val mediaItem: MediaItem) {
    actual val mediaId: String = mediaItem.mediaId
}

