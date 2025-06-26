package dev.halim.shelfdroid.core.ui.media3

import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

  private lateinit var mediaLibrarySession: MediaLibrarySession
  @Inject lateinit var player: Lazy<ExoPlayer>

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
    return mediaLibrarySession
  }

  override fun onCreate() {
    Log.d("media3", "PlaybackService onCreate called")
    super.onCreate()
    mediaLibrarySession =
      MediaLibrarySession.Builder(this, player.get(), object : MediaLibrarySession.Callback {})
        .build()
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
