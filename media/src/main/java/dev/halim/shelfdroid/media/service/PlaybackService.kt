package dev.halim.shelfdroid.media.service

import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

  private lateinit var mediaLibrarySession: MediaLibrarySession
  @Inject lateinit var playerManager: Lazy<ExoPlayerManager>
  @Inject lateinit var mediaNotificationProvider: Lazy<CustomMediaNotificationProvider>
  @Inject lateinit var mediaLibrarySessionCallback: Lazy<MediaLibrarySession.Callback>
  @Inject lateinit var helper: Lazy<Helper>

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
    mediaLibrarySession.release()
    playerManager.get().clearAndStop()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    Process.killProcess(Process.myPid())
    super.onDestroy()
  }

  @OptIn(UnstableApi::class)
  override fun onTaskRemoved(rootIntent: Intent?) {
    pauseAllPlayersAndStopSelf()
  }
}
