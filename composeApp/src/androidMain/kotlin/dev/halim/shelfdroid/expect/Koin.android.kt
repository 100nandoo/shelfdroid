package dev.halim.shelfdroid.expect

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.bundle.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_SKIP_BACK_10
import androidx.media3.session.CommandButton.ICON_SKIP_FORWARD_10
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dev.halim.shelfdroid.MainActivity
import dev.halim.shelfdroid.PlaybackService
import dev.halim.shelfdroid.R
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import org.koin.dsl.module

@SuppressLint("UnsafeOptInUsageError")
actual val targetModule = module {
    single<PlatformPlayer> { (serviceContext: PlaybackService) ->
        val player = ExoPlayer.Builder(serviceContext)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()

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

        val commandButtons = initCommandButtons()
        val mediaNotificationSessionCommands = buildMediaNotificationCommands(commandButtons)
        @UnstableApi
        object : MediaLibrarySession.Callback {
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                return handlePlaybackResumption(dataStoreManager)
            }

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                return buildConnectionResult(session, commandButtons, mediaNotificationSessionCommands)
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: android.os.Bundle
            ): ListenableFuture<SessionResult> {
                return handleCustomCommand(session, customCommand)
            }
        }
    }
    single<MediaLibrarySession> { (serviceContext: PlaybackService) ->
        val sessionActivityPendingIntent = pendingIntent(serviceContext)

        MediaLibrarySession.Builder(serviceContext, get(), get())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }
    single<MediaNotification.Provider> { (serviceContext: PlaybackService) ->
        @UnstableApi
        class CustomMediaNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {
            init {
                setSmallIcon(R.drawable.ic_notification)
            }

            override fun addNotificationActions(
                mediaSession: MediaSession,
                mediaButtons: ImmutableList<CommandButton>,
                builder: NotificationCompat.Builder,
                actionFactory: MediaNotification.ActionFactory
            ): IntArray {
                val playPause = mediaButtons.firstOrNull { it.playerCommand == COMMAND_PLAY_PAUSE }
                playPause?.extras?.putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 1)
                val notificationMediaButtons = if (playPause != null) {
                    ImmutableList.builder<CommandButton>().apply {
                        add(backCommandButton)
                        add(playPause)
                        add(forwardCommandButton)
                    }.build()
                } else {
                    mediaButtons
                }
                return super.addNotificationActions(
                    mediaSession,
                    notificationMediaButtons,
                    builder,
                    actionFactory
                )
            }
        }
        CustomMediaNotificationProvider(serviceContext)
    }
}

const val CUSTOM_BACK = "CUSTOM_BACK"
const val CUSTOM_FORWARD = "CUSTOM_FORWARD"

@SuppressLint("UnsafeOptInUsageError")
val backCommandButton = CommandButton.Builder(ICON_SKIP_BACK_10)
    .setSessionCommand(SessionCommand(CUSTOM_BACK, Bundle()))
    .setDisplayName(CUSTOM_BACK)
    .setExtras(Bundle().apply {
        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 0)
    })
    .build()

@SuppressLint("UnsafeOptInUsageError")
val forwardCommandButton = CommandButton.Builder(ICON_SKIP_FORWARD_10)
    .setSessionCommand(SessionCommand(CUSTOM_BACK, Bundle()))
    .setDisplayName(CUSTOM_FORWARD)
    .setExtras(Bundle().apply {
        putInt(DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX, 2)
    }).build()


@OptIn(UnstableApi::class)
private fun initCommandButtons(): List<CommandButton> {
    return listOf(backCommandButton, forwardCommandButton)
}

@OptIn(UnstableApi::class)
private fun buildMediaNotificationCommands(buttons: List<CommandButton>): SessionCommands {
    return MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
        .apply { buttons.forEach { commandButton -> commandButton.sessionCommand?.let { add(it) } } }.build()
}

@OptIn(UnstableApi::class)
private fun handlePlaybackResumption(
    dataStoreManager: DataStoreManager
): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
    val settableFuture = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
    println("handlePlaybackResumption")
    val uiState = dataStoreManager.readSerializableBlocking(::BookUiState.name, BookUiState.serializer())
    val mediaItems = mutableListOf<MediaItem>()
    uiState?.toMediaItem()?.let { mediaItems.add(it) }

    val startPosition = dataStoreManager.currentPositionBlocking
    val mediaItemsWithPosition = MediaSession.MediaItemsWithStartPosition(mediaItems, 0, startPosition)

    settableFuture.set(mediaItemsWithPosition)
    return settableFuture
}

@OptIn(UnstableApi::class)
private fun buildConnectionResult(
    session: MediaSession,
    buttons: List<CommandButton>,
    commands: SessionCommands
): MediaSession.ConnectionResult {
    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        .setAvailableSessionCommands(commands)
        .setCustomLayout(buttons)
        .build()
}

private fun handleCustomCommand(
    session: MediaSession,
    customCommand: SessionCommand
): ListenableFuture<SessionResult> {
    when (customCommand.customAction) {
        CUSTOM_BACK -> session.player.seekTo(session.player.currentPosition - 10000)
        CUSTOM_FORWARD -> session.player.seekTo(session.player.currentPosition + 10000)
    }
    return SettableFuture.create<SessionResult>().apply { set(SessionResult(SessionResult.RESULT_SUCCESS)) }
}

private fun pendingIntent(serviceContext: PlaybackService): PendingIntent {
    val mainActivityIntent = Intent(serviceContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        serviceContext,
        0,
        mainActivityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}