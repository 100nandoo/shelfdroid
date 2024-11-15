package dev.halim.shelfdroid

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import dev.halim.shelfdroid.expect.PlatformPlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf


class PlaybackService : MediaLibraryService(), KoinComponent {
    private lateinit var mediaSession: MediaLibrarySession

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        get<PlatformPlayer> { parametersOf(this) }
        mediaSession = get { parametersOf(this) }
        get<MediaNotification.Provider> { parametersOf(this) }
        setMediaNotificationProvider(get())
    }

    override fun onDestroy() {
        println("PlaybackService onDestroy")
        mediaSession.run {
            release()
            player.release()
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaSession
    }
}