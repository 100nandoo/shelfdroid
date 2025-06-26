package dev.halim.shelfdroid.core.ui.di

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.ui.media3.PlaybackService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

  @OptIn(UnstableApi::class)
  @Singleton
  @Provides
  fun providesPlayer(@ApplicationContext context: Context): ExoPlayer {
    val audioAttributes =
      AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .build()

    val audioOffloadPreferences =
      TrackSelectionParameters.AudioOffloadPreferences.Builder()
        .setAudioOffloadMode(
          TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
        )
        .setIsGaplessSupportRequired(true)
        .setIsSpeedChangeSupportRequired(true)
        .build()

    val player =
      ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setSeekBackIncrementMs(10000)
        .setSeekForwardIncrementMs(10000)
        .build()

    player.trackSelectionParameters
      .buildUpon()
      .setAudioOffloadPreferences(audioOffloadPreferences)
      .build()

    return player
  }

  @Singleton
  @Provides
  fun provideSessionToken(@ApplicationContext context: Context): SessionToken {
    return SessionToken(context, ComponentName(context, PlaybackService::class.java))
  }

  @Singleton
  @Provides
  fun provideMediaControllerFuture(
    @ApplicationContext context: Context,
    sessionToken: SessionToken,
  ): ListenableFuture<MediaController> {
    return MediaController.Builder(context, sessionToken).buildAsync()
  }
}
