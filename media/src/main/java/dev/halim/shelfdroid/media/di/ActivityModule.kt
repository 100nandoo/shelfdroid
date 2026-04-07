package dev.halim.shelfdroid.media.di

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dev.halim.shelfdroid.media.service.PlaybackService

@OptIn(UnstableApi::class)
@Module
@InstallIn(ActivityRetainedComponent::class)
object ActivityModule {

  @Provides
  @ActivityRetainedScoped
  fun provideSessionToken(@ApplicationContext context: Context): SessionToken {
    return SessionToken(context, ComponentName(context, PlaybackService::class.java))
  }

  @Provides
  @ActivityRetainedScoped
  fun provideMediaControllerFuture(
    @ApplicationContext context: Context,
    sessionToken: SessionToken,
  ): ListenableFuture<MediaController> {
    return MediaController.Builder(context, sessionToken).buildAsync()
  }
}
