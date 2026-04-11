package dev.halim.shelfdroid.media.service

import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.service.CustomMediaNotificationProvider.Companion.BACK_COMMAND_BUTTON
import dev.halim.shelfdroid.media.service.CustomMediaNotificationProvider.Companion.FORWARD_COMMAND_BUTTON
import dev.halim.shelfdroid.media.service.CustomMediaNotificationProvider.Companion.SLEEP_TIMER_OFF_BUTTON
import dev.halim.shelfdroid.media.service.CustomMediaNotificationProvider.Companion.SLEEP_TIMER_ON_BUTTON
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
  private var sleepTimerObserverJob: Job? = null

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
    observeSleepTimerState()
  }

  private fun observeSleepTimerState() {
    sleepTimerObserverJob?.cancel()
    sleepTimerObserverJob = serviceScope.launch {
      playerStore
        .get()
        .uiState
        .map { it.advancedControl.sleepTimerLeft > Duration.ZERO }
        .distinctUntilChanged()
        .collect { isActive ->
          val sleepButton = if (isActive) SLEEP_TIMER_ON_BUTTON else SLEEP_TIMER_OFF_BUTTON
          mediaLibrarySession.setMediaButtonPreferences(
            ImmutableList.of(BACK_COMMAND_BUTTON, FORWARD_COMMAND_BUTTON, sleepButton)
          )
        }
    }
  }

  private fun setupPlayerListener() {
    playerManager
      .get()
      .addListener(
        object : Player.Listener {
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
    sleepTimerObserverJob?.cancel()
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
