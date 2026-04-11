package dev.halim.shelfdroid.media.exoplayer

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PlayerEvent {
  object Pause : PlayerEvent

  object Resume : PlayerEvent
}

@Singleton
class ExoPlayerManager @Inject constructor() {

  @Inject lateinit var player: Lazy<ExoPlayer>

  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val _events = MutableSharedFlow<PlayerEvent>()
  val events: SharedFlow<PlayerEvent> = _events
  @Volatile private var isItemChanged = false

  private val listener =
    object : Player.Listener {
      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        isItemChanged = true
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        if (isPlaying) {
          emit(PlayerEvent.Resume)
          isItemChanged = false
        } else {
          emit(PlayerEvent.Pause)
        }
      }
    }

  fun isPlaying() = player.get().isPlaying

  fun isItemChanged() = isItemChanged

  suspend fun isPlayingSafe(): Boolean =
    withContext(Dispatchers.Main) {
      val result = player.get().isPlaying
      return@withContext result
    }

  suspend fun currentPosition(): Long {
    return withContext(Dispatchers.Main) { player.get().currentPosition }
  }

  fun addListener(listener: Player.Listener) {
    player.get().addListener(listener)
  }

  fun addDefaultListener() {
    player.get().addListener(listener)
  }

  fun currentMediaItem(): MediaItem? {
    return player.get().currentMediaItem
  }

  private fun emit(event: PlayerEvent) {
    syncScope.launch { _events.emit(event) }
  }
}
