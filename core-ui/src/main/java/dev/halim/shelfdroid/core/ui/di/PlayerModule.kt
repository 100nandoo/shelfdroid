package dev.halim.shelfdroid.core.ui.di

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.ui.media3.CUSTOM_BACK
import dev.halim.shelfdroid.core.ui.media3.CUSTOM_FORWARD
import dev.halim.shelfdroid.core.ui.media3.CustomMediaNotificationProvider
import dev.halim.shelfdroid.core.ui.media3.CustomMediaNotificationProvider.Companion.BACK_COMMAND_BUTTON
import dev.halim.shelfdroid.core.ui.media3.CustomMediaNotificationProvider.Companion.FORWARD_COMMAND_BUTTON
import dev.halim.shelfdroid.core.ui.media3.PlaybackService
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

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

  @Singleton
  @Provides
  fun provideMediaNotificationProvider(
    @ApplicationContext context: Context
  ): CustomMediaNotificationProvider {
    return CustomMediaNotificationProvider(context)
  }

  @Singleton
  @Provides
  fun provideMediaLibrarySessionCallback(): MediaLibrarySession.Callback {

    val commandButtons = listOf(BACK_COMMAND_BUTTON, FORWARD_COMMAND_BUTTON)
    val sessionCommands =
      MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        .apply {
          commandButtons.forEach { commandButton -> commandButton.sessionCommand?.let { add(it) } }
        }
        .build()
    return object : MediaLibrarySession.Callback {
      override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
      ): MediaSession.ConnectionResult {
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
          .setAvailableSessionCommands(sessionCommands)
          .setCustomLayout(commandButtons)
          .build()
      }

      override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
      ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
          CUSTOM_BACK -> session.player.seekTo(session.player.currentPosition - 10000)
          CUSTOM_FORWARD -> session.player.seekTo(session.player.currentPosition + 10000)
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
      }
    }
  }
}
