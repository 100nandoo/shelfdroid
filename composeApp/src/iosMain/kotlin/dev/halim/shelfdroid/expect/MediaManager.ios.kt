package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class MediaManager actual constructor(playerWrapper: PlayerWrapper) {
    private val _playerState = MutableStateFlow(MediaPlayerState())
    actual val playerState = _playerState.asStateFlow()
    actual fun playBookUiState(uiState: BookUiState) {
    }

    actual fun release() {
    }
}

actual class MediaItemWrapper {
    actual val mediaId: String = ""
}

actual class PlayerWrapper