package dev.halim.shelfdroid.media.misc

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.core.network.connectivity.NetworkMonitor
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.SYNC_LONG_INTERVAL
import dev.halim.shelfdroid.core.data.screen.SYNC_SHORT_INTERVAL
import dev.halim.shelfdroid.core.data.session.LocalSessionRepo
import dev.halim.shelfdroid.core.data.session.RemoteSessionRepo
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class SessionManager
@Inject
constructor(
  private val player: Lazy<ExoPlayer>,
  private val localSessionRepo: LocalSessionRepo,
  private val remoteSessionRepo: RemoteSessionRepo,
  private val networkMonitor: NetworkMonitor,
) {
  private var duration = Duration.ZERO

  private var syncJob: Job? = null
  private var isPlaying = false
  private var isLocal = false
  private var uiState: PlayerUiState = PlayerUiState()
  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  @Volatile private var isMetered = true
  @Volatile private var isSyncing = false
  @Volatile private var isItemChanged = false

  init {
    syncScope.launch {
      combine(networkMonitor.status, localSessionRepo.count()) { status, count ->
          isMetered = status.isMetered
          status.hasInternet && (count ?: 0) > 0
        }
        .collect { shouldSync -> if (shouldSync) localSessionRepo.syncToServer() }
    }
  }

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

    isLocal = this.uiState.download.state.isDownloaded()
    if (isLocal) {
      syncScope.launch { localSessionRepo.startBook(uiState) }
    }
  }

  private val listener =
    object : Player.Listener {
      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        isItemChanged = true
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        this@SessionManager.isPlaying = isPlaying
        if (isPlaying) {
          startSyncJob()
          isItemChanged = false
        } else {
          syncJob?.cancel()
          sync()
        }
      }
    }

  private fun resetDuration() {
    duration = 0.seconds
  }

  private suspend fun getCurrentPositionSafe(): Long {
    return withContext(Dispatchers.Main) { player.get().currentPosition }
  }

  private fun sync() {
    if (isSyncing || isItemChanged || duration == 0.seconds) return
    syncScope.launch {
      isSyncing = true
      if (isLocal) {
        localSessionRepo.syncLocal(uiState, getCurrentPositionSafe())
      } else {
        remoteSessionRepo.syncSession(uiState, getCurrentPositionSafe(), duration)
      }
      resetDuration()
      isSyncing = false
    }
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
            sync()
          }
        }
      }
  }
}
