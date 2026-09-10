package dev.halim.shelfdroid.media.di

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.media.service.CUSTOM_BACK
import dev.halim.shelfdroid.media.service.CUSTOM_FORWARD
import dev.halim.shelfdroid.media.service.CUSTOM_NEXT_CHAPTER
import dev.halim.shelfdroid.media.service.CUSTOM_PLAYBACK_SPEED
import dev.halim.shelfdroid.media.service.CUSTOM_SLEEP_TIMER
import dev.halim.shelfdroid.media.service.CustomMediaNotificationProvider
import dev.halim.shelfdroid.media.service.MediaNotificationButtons.BACK_COMMAND_BUTTON
import dev.halim.shelfdroid.media.service.MediaNotificationButtons.FORWARD_COMMAND_BUTTON
import dev.halim.shelfdroid.media.service.MediaNotificationButtons.PLAYBACK_SPEED_BUTTON
import dev.halim.shelfdroid.media.service.MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON
import dev.halim.shelfdroid.media.service.PlayerStore
import dev.halim.shelfdroid.media.service.mediaNotificationButtons
import javax.inject.Singleton
import kotlin.time.Duration

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

  @Singleton
  @Provides
  fun provideMediaSourceFactory(
    @ApplicationContext context: Context,
    okhttpDataSourceFactory: DataSource.Factory,
    cache: Cache,
  ): MediaSource.Factory {
    val upstreamDataSourceFactory = DefaultDataSource.Factory(context, okhttpDataSourceFactory)
    val cacheDataSourceFactory =
      CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)

    return DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory)
  }

  @Singleton
  @Provides
  fun providesPlayer(
    @ApplicationContext context: Context,
    mediaSourceFactory: MediaSource.Factory,
  ): ExoPlayer {
    Log.d("media3", "exoplayer instantiated")

    val audioOnlyRenderersFactory =
      RenderersFactory {
        handler: Handler,
        _: VideoRendererEventListener,
        audioListener: AudioRendererEventListener,
        _: TextOutput,
        _: MetadataOutput ->
        arrayOf<Renderer>(
          MediaCodecAudioRenderer(context, MediaCodecSelector.DEFAULT, handler, audioListener)
        )
      }

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
      ExoPlayer.Builder(context, audioOnlyRenderersFactory)
        .setMediaSourceFactory(mediaSourceFactory)
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
  fun provideMediaNotificationProvider(
    @ApplicationContext context: Context
  ): CustomMediaNotificationProvider {
    return CustomMediaNotificationProvider(context)
  }

  @Singleton
  @Provides
  fun provideMediaLibrarySessionCallback(
    @ApplicationContext context: Context,
    playerStore: Lazy<PlayerStore>,
  ): MediaLibrarySession.Callback {

    val sessionCommands =
      MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        .apply {
          listOf(
              BACK_COMMAND_BUTTON,
              FORWARD_COMMAND_BUTTON,
              SLEEP_TIMER_OFF_BUTTON,
              PLAYBACK_SPEED_BUTTON,
            )
            .forEach { commandButton ->
              commandButton.sessionCommand?.let { add(it) }
            }
          add(SessionCommand(CUSTOM_NEXT_CHAPTER, Bundle()))
        }
        .build()
    return object : MediaLibrarySession.Callback {

      override fun onConnectAsync(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
      ): ListenableFuture<MediaSession.ConnectionResult> {
        val store = playerStore.get()
        val commandButtons =
          mediaNotificationButtons(
            context.getString(CoreR.string.next_chapter),
            store.uiState.value,
            store.isChapterTransitioning.value,
            store.uiState.value.advancedControl.sleepTimerLeft > Duration.ZERO,
            store.notificationPrefs.value,
            seekBackSeconds = store.playerPrefs.value.seekBackSeconds,
            seekForwardSeconds = store.playerPrefs.value.seekForwardSeconds,
          )
        return Futures.immediateFuture(
          MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            .setAvailableSessionCommands(sessionCommands)
            .setCustomLayout(commandButtons)
            .setMediaButtonPreferences(commandButtons)
            .build()
        )
      }

      override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
      ): ListenableFuture<SessionResult> {
        val resultCode =
          when (customCommand.customAction) {
            CUSTOM_BACK -> {
              session.player.seekBack()
              SessionResult.RESULT_SUCCESS
            }
            CUSTOM_FORWARD -> {
              session.player.seekForward()
              SessionResult.RESULT_SUCCESS
            }
            CUSTOM_SLEEP_TIMER -> {
              val store = playerStore.get()
              if (store.uiState.value.advancedControl.sleepTimerLeft > Duration.ZERO) {
                store.clearTimer()
              } else {
                store.startDefaultSleepTimer()
              }
              SessionResult.RESULT_SUCCESS
            }
            CUSTOM_NEXT_CHAPTER -> {
              if (playerStore.get().nextChapterFromMediaNotification()) {
                SessionResult.RESULT_SUCCESS
              } else {
                SessionResult.RESULT_ERROR_INVALID_STATE
              }
            }
            CUSTOM_PLAYBACK_SPEED -> {
              if (playerStore.get().changeSpeedFromMediaNotification()) {
                SessionResult.RESULT_SUCCESS
              } else {
                SessionResult.RESULT_ERROR_INVALID_STATE
              }
            }
            else -> SessionResult.RESULT_ERROR_NOT_SUPPORTED
          }
        return Futures.immediateFuture(SessionResult(resultCode))
      }
    }
  }
}
