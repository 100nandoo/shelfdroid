package dev.halim.shelfdroid.media.misc

import dev.halim.core.network.connectivity.NetworkMonitor
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.SYNC_LONG_INTERVAL
import dev.halim.shelfdroid.core.data.screen.SYNC_SHORT_INTERVAL
import dev.halim.shelfdroid.core.data.session.LocalSessionRepo
import dev.halim.shelfdroid.core.data.session.RemoteSessionRepo
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.exoplayer.PlayerEvent
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Singleton
class SessionManager
@Inject
constructor(
  private val exoPlayerManager: ExoPlayerManager,
  private val localSessionRepo: LocalSessionRepo,
  private val remoteSessionRepo: RemoteSessionRepo,
  private val networkMonitor: NetworkMonitor,
) {
  private var duration = Duration.ZERO

  private var syncJob: Job? = null
  private var isLocal = false
  private var uiState: PlayerUiState = PlayerUiState()
  private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  @Volatile private var isMetered = true
  @Volatile private var isSyncing = false

  init {
    syncScope.launch {
      combine(
          networkMonitor.status.map { it.hasInternet to it.isMetered }.distinctUntilChanged(),
          localSessionRepo.hasProgress(),
        ) { (hasInternet, isMetered), hasProgress ->
          (hasInternet && hasProgress) to isMetered
        }
        .filter { (shouldSync, _) -> shouldSync }
        .onEach { (_, isMeteredValue) ->
          isMetered = isMeteredValue
          localSessionRepo.syncToServer()
        }
        .launchIn(this)
    }

    syncScope.launch {
      exoPlayerManager.events.collect { event ->
        when (event) {
          PlayerEvent.Pause -> {
            syncJob?.cancel()
            sync()
          }
          PlayerEvent.Resume -> {
            startSyncJob()
          }
        }
      }
    }
  }

  fun start(uiState: PlayerUiState) {
    this.uiState = uiState
    resetDuration()

    startSyncJob()

    isLocal = this.uiState.downloadState.isDownloaded()
    if (isLocal) {
      syncScope.launch { localSessionRepo.start(uiState) }
    }
  }

  private fun resetDuration() {
    duration = 0.seconds
  }

  private fun sync() {
    if (isSyncing || duration == 0.seconds) return
    syncScope.launch {
      isSyncing = true
      if (isLocal) {
        localSessionRepo.syncLocal(uiState, exoPlayerManager.currentPosition(), duration)
      } else {
        remoteSessionRepo.syncSession(uiState, exoPlayerManager.currentPosition(), duration)
      }
      resetDuration()
      isSyncing = false
    }
  }

  private fun startSyncJob() {
    syncJob?.cancel()
    syncJob =
      syncScope.launch {
        while (true) {
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
