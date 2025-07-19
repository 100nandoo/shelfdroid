package dev.halim.shelfdroid.media.di

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
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

  @Provides
  fun provideMediaControllerFuture(
    @ApplicationContext context: Context,
    sessionToken: SessionToken,
  ): ListenableFuture<MediaController> {
    return MediaController.Builder(context, sessionToken).buildAsync()
  }
}

class MediaControllerManager
@Inject
constructor(val mediaControllerFuture: ListenableFuture<MediaController>) {

  var mediaController: MediaController? = null

  fun init(scope: CoroutineScope) {
    if (mediaController != null) return
    Log.d("media3", "mediaController instantiated")
    scope.launch { runCatching { mediaController = mediaControllerFuture.await() } }
  }

  fun release() {
    mediaController?.release()
  }
}
