package dev.halim.shelfdroid.media.di

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.media.service.PlaybackService
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

  @Provides
  fun provideSessionToken(@ApplicationContext context: Context): SessionToken {
    return SessionToken(context, ComponentName(context, PlaybackService::class.java))
  }
}

class MediaControllerManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val sessionToken: SessionToken,
) {

  var mediaController: MediaController? = null
    private set

  fun init(scope: CoroutineScope) {
    if (mediaController?.isConnected == true) {
      Log.d("media3", "MediaController already connected")
      return
    }

    Log.d("media3", "Creating / reconnecting MediaController")

    scope.launch {
      runCatching {
          mediaController?.release()
          val future = MediaController.Builder(context, sessionToken).buildAsync()
          mediaController = future.await()
          Log.d("media3", "MediaController connected=${mediaController?.isConnected}")
        }
        .onFailure { Log.e("media3", "Failed to init MediaController", it) }
    }
  }

  fun release() {
    mediaController?.release()
    mediaController = null
  }
}
