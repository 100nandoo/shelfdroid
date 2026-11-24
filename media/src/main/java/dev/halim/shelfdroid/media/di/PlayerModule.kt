package dev.halim.shelfdroid.media.di

import android.content.Context
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.media.notification.CustomMediaNotificationProvider
import javax.inject.Singleton

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

    player.trackSelectionParameters =
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
}
