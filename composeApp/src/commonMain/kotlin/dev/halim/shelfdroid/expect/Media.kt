package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

expect class PlatformMediaItem {
    val id: String
}

expect class MediaManager(
    player: PlatformPlayer, dataStoreManager: DataStoreManager,
    sessionManager: SessionManager
) {
    val playerState: StateFlow<MediaPlayerState>
    fun playBookUiState(item: ShelfdroidMediaItemImpl)
    fun seekForward()
    fun seekBackward()
    fun changeSpeed(speed: Float)
}

data class MediaPlayerState(
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val item: PlatformMediaItem? = null,
    val currentPosition: Flow<Long> = flow { }
)

sealed class PlaybackState {
    data object Playing : PlaybackState()
    data object Pause : PlaybackState()
    data object Buffering : PlaybackState()
    data object Idle : PlaybackState()
    data object Ended : PlaybackState()
}

expect interface PlatformPlayer

