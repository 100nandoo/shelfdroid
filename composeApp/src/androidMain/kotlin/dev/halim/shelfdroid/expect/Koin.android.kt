package dev.halim.shelfdroid.expect

import android.annotation.SuppressLint
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.halim.shelfdroid.PlaybackService
import dev.halim.shelfdroid.datastore.createDataStore
import org.koin.dsl.module

@SuppressLint("UnsafeOptInUsageError")
actual val targetModule = module {
    single<DataStore<Preferences>> { createDataStore(get()) }
    single<PlatformPlayer> { (serviceContext: PlaybackService) ->
        println("targetModule android PlatformPlayer")
        val player = ExoPlayer.Builder(serviceContext).build()

        val audioOffloadPreferences =
            TrackSelectionParameters.AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                .setIsGaplessSupportRequired(true)
                .build()
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPreferences)
                .build()
        player
    }
    single<MediaLibrarySession.Callback> {
        @UnstableApi
        object : MediaLibrarySession.Callback {
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val settable = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                return settable
            }
        }
    }
    single<MediaLibrarySession> { (serviceContext: PlaybackService) ->
        println("targetModule android MediaLibrarySession")
        MediaLibrarySession.Builder(serviceContext, get(), get())
            .build()
    }
}