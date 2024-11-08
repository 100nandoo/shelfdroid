package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class MediaManager {
    private val _playerState = MutableStateFlow(MediaPlayerState())
    actual val playerState = _playerState.asStateFlow()

    actual fun addItem(uiState: BookUiState) {
    }

    actual fun currentItem(): MediaItemWrapper? {
        return null
    }


    actual fun play() {
    }

    actual fun pause() {
    }

    actual fun release() {
    }

    actual fun isPlaying(): Boolean = false

}

actual class MediaItemWrapper {
    actual val mediaId: String = ""
}