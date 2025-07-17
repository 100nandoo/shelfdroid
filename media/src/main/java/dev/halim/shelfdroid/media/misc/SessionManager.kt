package dev.halim.shelfdroid.media.misc

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.core.network.connectivity.ConnectivityManager
import dev.halim.shelfdroid.core.data.screen.SYNC_LONG_INTERVAL
import dev.halim.shelfdroid.core.data.screen.SYNC_SHORT_INTERVAL
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
constructor(
  private val player: Lazy<ExoPlayer>,
  private val playerRepository: PlayerRepository,
  private val connectivityObserver: ConnectivityManager,
) {
  private var duration = Duration.ZERO
  private var syncJob: Job? = null
  private var connectivityJob: Job? = null
  private var isPlaying = false
  private lateinit var uiState: PlayerUiState
  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  @Volatile private var isMetered = true

  fun start(uiState: PlayerUiState) {
    this.uiState = uiState
    resetDuration()

    player.get().apply {
      removeListener(listener)
      addListener(listener)
      this@SessionManager.isPlaying = isPlaying
    }

    if (isPlaying) {
      startConnectivityJob()
      startSyncJob()
    }
  }

  private val listener =
    object : Player.Listener {
      override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        this@SessionManager.isPlaying = isPlaying
        if (isPlaying) {
          startConnectivityJob()
          startSyncJob()
        } else {
          connectivityJob?.cancel()
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
          val syncThreshold = if (isMetered) SYNC_LONG_INTERVAL else SYNC_SHORT_INTERVAL
          if (duration >= syncThreshold.seconds) {
            syncToServer()
          }
        }
      }
  }

  private fun startConnectivityJob() {
    isMetered = connectivityObserver.currentStatus().isMetered
    connectivityJob?.cancel()
    connectivityJob =
      syncScope.launch {
        connectivityObserver.observe().collect { status -> isMetered = status.isMetered }
      }
  }
}
