package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class MediaManager actual constructor(player: PlatformPlayer) {
    private val _playerState = MutableStateFlow(MediaPlayerState())
    actual val playerState = _playerState.asStateFlow()
    actual fun playBookUiState(uiState: BookUiState) {}
}

actual class PlatformMediaItem {
    actual val id: String = ""
}

actual interface PlatformPlayer {
    companion object {
        val INSTANCE = object : PlatformPlayer {}
    }
}
