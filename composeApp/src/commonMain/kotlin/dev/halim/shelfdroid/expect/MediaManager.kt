package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.StateFlow

expect class MediaItemWrapper {
    val mediaId: String
}

expect class PlayerWrapper

expect class MediaManager(playerWrapper: PlayerWrapper) {
    val playerState: StateFlow<MediaPlayerState>
    fun playBookUiState(uiState: BookUiState)
    fun release()
}

data class MediaPlayerState(
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val currentMediaItemWrapper: MediaItemWrapper? = null,
)

sealed class PlaybackState {
    data object Playing : PlaybackState()
    data object Pause : PlaybackState()
    data object Buffering : PlaybackState()
    data object Idle: PlaybackState()
    data object Ended: PlaybackState()
}