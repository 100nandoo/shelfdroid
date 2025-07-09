package dev.halim.shelfdroid.media.service

import android.app.PendingIntent
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
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

  private lateinit var mediaLibrarySession: MediaLibrarySession
  @Inject lateinit var player: Lazy<ExoPlayer>
  @Inject lateinit var mediaNotificationProvider: Lazy<CustomMediaNotificationProvider>
  @Inject lateinit var mediaLibrarySessionCallback: Lazy<MediaLibrarySession.Callback>

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
                val intent = createIntent(it)
                Log.d("media3", "onIsPlayingChanged: $it")
                mediaLibrarySession.setSessionActivity(intent)
              }
            }
          }
        }
      )
  }

  private fun createIntent(mediaId: String): PendingIntent {
    val intent = Intent(ACTION_OPEN_PLAYER)
    intent.apply {
      setPackage(applicationContext.packageName)
      putExtra(EXTRA_MEDIA_ID, mediaId)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
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

  companion object {
    const val ACTION_OPEN_PLAYER = "dev.halim.shelfdroid.OPEN_PLAYER"
    const val EXTRA_MEDIA_ID = "media_id"
  }
}
