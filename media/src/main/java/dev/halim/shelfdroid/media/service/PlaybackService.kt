package dev.halim.shelfdroid.media.service

import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

  private lateinit var mediaLibrarySession: MediaLibrarySession
  @Inject lateinit var player: Lazy<ExoPlayer>
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
      MediaLibrarySession.Builder(this, player.get(), mediaLibrarySessionCallback.get()).build()

    setupPlayerListener()
    setMediaNotificationProvider(mediaNotificationProvider.get())
  }

  @OptIn(UnstableApi::class)
  private fun setupPlayerListener() {
    player
      .get()
      .addListener(
        object : Player.Listener {
          override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (isPlaying) {
              player.get().currentMediaItem?.mediaId?.let {
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
    player.get().apply { clearMediaItems() }
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
