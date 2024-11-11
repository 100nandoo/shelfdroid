package dev.halim.shelfdroid.expect

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.halim.shelfdroid.MainActivity
import dev.halim.shelfdroid.PlaybackService
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.createDataStore
import dev.halim.shelfdroid.ui.screens.home.BookUiState
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
        val dataStoreManager by inject<DataStoreManager>()
        @UnstableApi
        object : MediaLibrarySession.Callback {
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val settableFuture = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

                val uiState = dataStoreManager.readSerializableBlocking(::BookUiState.name, BookUiState.serializer())
                val mediaItems = mutableListOf<MediaItem>()

                uiState?.toMediaItem()?.let { mediaItems.add(it) }

                val startPosition = dataStoreManager.currentPositionBlocking
                val mediaItemsWithPosition = MediaSession.MediaItemsWithStartPosition(mediaItems, 0, startPosition)

                settableFuture.set(mediaItemsWithPosition)
                return settableFuture
            }
        }
    }
    single<MediaLibrarySession> { (serviceContext: PlaybackService) ->
        println("targetModule android MediaLibrarySession")
        val sessionActivityPendingIntent = pendingIntent(serviceContext)

        MediaLibrarySession.Builder(serviceContext, get(), get())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }
}

// Handle on click push notification
private fun pendingIntent(serviceContext: PlaybackService): PendingIntent {
    val mainActivityIntent = Intent(serviceContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val sessionActivityPendingIntent = PendingIntent.getActivity(
        serviceContext,
        0,
        mainActivityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return sessionActivityPendingIntent
}