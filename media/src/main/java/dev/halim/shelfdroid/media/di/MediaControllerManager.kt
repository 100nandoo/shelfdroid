package dev.halim.shelfdroid.media.di

import android.util.Log
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

  var mediaController: MediaController? = null
    private set

  fun init(scope: CoroutineScope) {
    Log.d("media3", "MediaController init called.")
    if (mediaController?.isConnected == true) {
      Log.d("media3", "MediaController already connected")
      return
    }

    Log.d("media3", "Creating / reconnecting MediaController")

    scope.launch {
      runCatching {
          mediaController?.release()
          mediaController = mediaControllerFuture.await()
          Log.d("media3", "MediaController connected=${mediaController?.isConnected}")
        }
        .onFailure { Log.e("media3", "Failed to init MediaController", it) }
    }
  }

  fun seekBack() {
    mediaController?.seekBack()
  }

  fun seekForward() {
    mediaController?.seekForward()
  }

  fun playPause() {
    if (mediaController?.isPlaying == true) {
      mediaController?.pause()
    } else {
      mediaController?.play()
    }
  }

  fun release() {
    Log.d("media3", "MediaController release.")
    mediaController?.release()
    mediaController = null
  }
}
