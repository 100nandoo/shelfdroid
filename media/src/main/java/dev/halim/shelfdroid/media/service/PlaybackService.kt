package dev.halim.shelfdroid.media.service

import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

  private lateinit var mediaLibrarySession: MediaLibrarySession
  @Inject lateinit var playerManager: Lazy<ExoPlayerManager>
  @Inject lateinit var mediaNotificationProvider: Lazy<CustomMediaNotificationProvider>
  @Inject lateinit var mediaLibrarySessionCallback: Lazy<MediaLibrarySession.Callback>
  @Inject lateinit var helper: Lazy<Helper>
  @Inject lateinit var playerStore: Lazy<PlayerStore>

  private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var mediaButtonObserverJob: Job? = null

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
    return mediaLibrarySession
  }

  override fun onCreate() {
    Log.d("media3", "PlaybackService onCreate called")
    super.onCreate()

    mediaLibrarySession =
      MediaLibrarySession.Builder(
          this,
          playerManager.get().player.get(),
          mediaLibrarySessionCallback.get(),
        )
        .build()

    setupPlayerListener()
    playerManager.get().addDefaultListener()
    setMediaNotificationProvider(mediaNotificationProvider.get())
    observeMediaButtonState()
  }

  private fun observeMediaButtonState() {
    mediaButtonObserverJob?.cancel()
    val store = playerStore.get()
    mediaButtonObserverJob = serviceScope.launch {
      combine(
          store.uiState,
          store.isChapterTransitioning,
          store.uiState
            .map { it.advancedControl.sleepTimerLeft > Duration.ZERO }
            .distinctUntilChanged(),
          store.notificationPrefs,
        ) { uiState, isTransitioning, isSleepTimerActive, notificationPrefs ->
          MediaNotificationButtonState(
            nextChapterControlState(uiState, isTransitioning),
            isSleepTimerActive,
            uiState.advancedControl.speed,
            notificationPrefs,
          )
        }
        .distinctUntilChanged()
        .collect { state ->
          mediaLibrarySession.setMediaButtonPreferences(
            mediaNotificationButtons(
              getString(CoreR.string.next_chapter),
              state.nextChapterState,
              state.isSleepTimerActive,
              state.notificationPrefs,
              state.playbackSpeed,
            )
          )
        }
    }
  }

  private fun setupPlayerListener() {
    playerManager
      .get()
      .addListener(
        object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
              playerStore.get().completeChapterTransition()
            }
          }

          override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            playerStore.get().completeChapterTransition()
          }

          override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying) {
              playerManager.get().currentMediaItem()?.mediaId?.let {
                val intent = helper.get().createOpenPlayerIntent(it, this@PlaybackService)
                Log.d("media3", "onIsPlayingChanged: $it")
                mediaLibrarySession.setSessionActivity(intent)
              }
            }
          }
        }
      )
  }

  override fun onDestroy() {
    mediaButtonObserverJob?.cancel()
    stopAndClear()
    mediaLibrarySession.release()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    Process.killProcess(Process.myPid())
    super.onDestroy()
  }

  @OptIn(UnstableApi::class)
  override fun onTaskRemoved(rootIntent: Intent?) {
    pauseAllPlayersAndStopSelf()
  }

  private fun stopAndClear() {
    mediaLibrarySession.player.run {
      stop()
      clearMediaItems()
    }
  }
}
