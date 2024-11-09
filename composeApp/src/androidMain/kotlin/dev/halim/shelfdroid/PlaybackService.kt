package dev.halim.shelfdroid

import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dev.halim.shelfdroid.expect.PlatformPlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class PlaybackService : MediaLibraryService(), KoinComponent {
    private lateinit var player: PlatformPlayer
    private lateinit var mediaSession: MediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        val context: Context = this
        player = get { parametersOf(context) }
        mediaSession = get { parametersOf(context) }
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaSession
    }
}