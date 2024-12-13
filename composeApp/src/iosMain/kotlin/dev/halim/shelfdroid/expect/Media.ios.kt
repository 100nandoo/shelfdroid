package dev.halim.shelfdroid.expect

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.player.SessionManager
import dev.halim.shelfdroid.player.Timer
import dev.halim.shelfdroid.ui.MediaItemBook
import dev.halim.shelfdroid.ui.MediaItemPodcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

actual class MediaManager actual constructor(
    player: PlatformPlayer, dataStoreManager: DataStoreManager,
    sessionManager: SessionManager, timer: Timer,
    private val main: CoroutineScope
) {
    private val _playerState = MutableStateFlow(MediaPlayerState())
    actual val playerState = _playerState.asStateFlow()
    actual fun playBook(item: MediaItemBook) {}

    actual fun changeChapter(item: MediaItemBook) {
    }

    actual fun playPodcast(item: MediaItemPodcast) {
    }

    actual fun seekForward() {
        TODO()
    }

    actual fun seekBackward() {
        TODO()
    }

    actual fun changeSpeed(speed: Float) {
        TODO()
    }

    actual fun seekTo(positionMs: Long) {
    }

    actual fun setSleepTimer(duration: Duration) {
    }

    actual val currentPosition: Flow<Long>
        get() = TODO("Not yet implemented")

    actual fun pause() {
    }


    actual fun play() {
    }

}

actual interface PlatformPlayer {
    companion object {
        val INSTANCE = object : PlatformPlayer {}
    }
}