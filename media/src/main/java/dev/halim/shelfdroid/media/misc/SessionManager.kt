package dev.halim.shelfdroid.media.misc

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.shelfdroid.core.data.screen.SYNC_INTERVAL
import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionManager
@Inject
constructor(private val player: Lazy<ExoPlayer>, private val playerRepository: PlayerRepository) {
  private var duration = Duration.ZERO
  private var syncJob: Job? = null
  private var isPlaying = false
  private lateinit var uiState: PlayerUiState
  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  fun start(uiState: PlayerUiState) {
    this.uiState = uiState
    resetDuration()

    player.get().apply {
      removeListener(listener)
      addListener(listener)
      this@SessionManager.isPlaying = isPlaying
    }

    if (isPlaying) {
      startSyncJob()
    }
  }

  private val listener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        this@SessionManager.isPlaying = isPlaying
        if (isPlaying) {
          startSyncJob()
        } else {
          syncJob?.cancel()
          syncToServer()
        }
      }
    }

  private fun resetDuration() {
    duration = 0.seconds
  }

  private suspend fun getCurrentPositionSafe(): Long {
    return withContext(Dispatchers.Main) { player.get().currentPosition }
  }

  private fun syncToServer() {
    val syncDuration = duration
    syncScope.launch {
      playerRepository.syncSession(uiState, getCurrentPositionSafe(), syncDuration)
    }
    resetDuration()
  }

  private fun startSyncJob() {
    syncJob?.cancel()
    syncJob =
      syncScope.launch {
        while (isPlaying) {
          delay(1.seconds)
          duration += 1.seconds
          Log.d("sync", duration.inWholeSeconds.toString())
          if (duration >= SYNC_INTERVAL.seconds) {
            syncToServer()
          }
        }
      }
  }
}
