package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.player.SessionManager
import dev.halim.shelfdroid.player.Timer
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

expect class MediaManager(
    player: PlatformPlayer, dataStoreManager: DataStoreManager, sessionManager: SessionManager, timer: Timer,
    main: CoroutineScope) {
    val playerState: StateFlow<MediaPlayerState>
    val currentPosition: Flow<Long>
    fun playBookUiState(item: ShelfdroidMediaItemImpl)
    fun changeChapter(item: ShelfdroidMediaItemImpl)
    fun play()
    fun pause()
    fun seekForward()
    fun seekBackward()
    fun seekTo(positionMs: Long)
    fun changeSpeed(speed: Float)
    fun setSleepTimer(duration: Duration)
}

data class MediaPlayerState(
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val item: ShelfdroidMediaItemImpl? = null,
    val sleepTimeLeft: Duration = Duration.ZERO
)

sealed class PlaybackState {
    data object Playing : PlaybackState()
    data object Pause : PlaybackState()
    data object Buffering : PlaybackState()
    data object Idle : PlaybackState()
    data object Ended : PlaybackState()
}

expect interface PlatformPlayer

