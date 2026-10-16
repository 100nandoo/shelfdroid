package dev.halim.shelfdroid.media.di

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@ActivityRetainedScoped
class MediaControllerManager
@Inject
constructor(private val controllerProvider: Provider<ListenableFuture<MediaController>>) {

  private var mediaController: MediaController? = null
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var connectionJob: Job? = null

  fun init(scope: CoroutineScope) {
    Log.d("media3", "MediaController init called.")
    if (mediaController?.isConnected == true || connectionJob?.isActive == true) {
      Log.d("media3", "MediaController already connected")
      return
    }

    Log.d("media3", "Creating / reconnecting MediaController")

    release()
    val future = controllerProvider.get()
    controllerFuture = future
    connectionJob = scope.launch {
      try {
        // releaseFuture owns cleanup even if the activity scope is cancelled while connecting.
        mediaController = Futures.nonCancellationPropagating(future).await()
        Log.d("media3", "MediaController connected=${mediaController?.isConnected}")
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        Log.e("media3", "Failed to init MediaController", exception)
      }
    }
  }

  fun seekBack() {
    mediaController?.seekBack()
  }

  fun seekForward() {
    mediaController?.seekForward()
  }

  fun playPause() {
    mediaController?.togglePlayback()
  }

  fun changeSpeed(speed: Float) {
    mediaController?.setPlaybackSpeed(speed)
  }

  fun seekTo(positionMs: Long) {
    mediaController?.apply {
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
    return mediaController?.currentPosition ?: 0L
  }

  fun clearAndStop() {
    mediaController?.apply {
      stop()
      clearMediaItems()
    }
  }

  fun release() {
    Log.d("media3", "MediaController release.")
    connectionJob?.cancel()
    connectionJob = null
    mediaController = null
    controllerFuture?.let(MediaController::releaseFuture)
    controllerFuture = null
  }
}

internal fun Player.togglePlayback() {
  if (Util.shouldShowPlayButton(this)) play() else pause()
}
