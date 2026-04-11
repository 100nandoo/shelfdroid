package dev.halim.shelfdroid.media.di

import android.util.Log
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@ActivityRetainedScoped
class MediaControllerManager
@Inject
constructor(val mediaControllerFuture: ListenableFuture<MediaController>) {

  lateinit var mediaController: MediaController
    private set

  fun init(scope: CoroutineScope) {
    Log.d("media3", "MediaController init called.")
    if (::mediaController.isInitialized && mediaController.isConnected) {
      Log.d("media3", "MediaController already connected")
      return
    }

    Log.d("media3", "Creating / reconnecting MediaController")

    scope.launch {
      runCatching {
          if (::mediaController.isInitialized) {
            mediaController.release()
          }
          mediaController = mediaControllerFuture.await()
          Log.d("media3", "MediaController connected=${mediaController.isConnected}")
        }
        .onFailure { Log.e("media3", "Failed to init MediaController", it) }
    }
  }

  fun seekBack() {
    mediaController.seekBack()
  }

  fun seekForward() {
    mediaController.seekForward()
  }

  fun playPause() {
    if (mediaController.isPlaying) {
      mediaController.pause()
    } else {
      mediaController.play()
    }
  }

  fun changeSpeed(speed: Float) {
    mediaController.setPlaybackSpeed(speed)
  }

  fun seekTo(positionMs: Long) {
    mediaController.apply {
      if (mediaItemCount > 1) {
        val window = Timeline.Window()
        var sum = 0L
        for (i in 0 until currentTimeline.windowCount) {
          currentTimeline.getWindow(i, window)
          val windowDuration = window.durationMs
          if (positionMs < sum + windowDuration) {
            seekTo(i, positionMs - sum)
            return
          }
          sum += windowDuration
        }
        currentTimeline.getWindow(currentTimeline.windowCount - 1, window)
        seekTo(currentTimeline.windowCount - 1, window.durationMs)
      } else {
        seekTo(positionMs)
      }
    }
  }

  fun currentPosition(): Long {
    return mediaController.currentPosition
  }

  fun clearAndStop() {
    mediaController.apply {
      stop()
      clearMediaItems()
    }
  }

  fun release() {
    Log.d("media3", "MediaController release.")
    mediaController.release()
  }
}
