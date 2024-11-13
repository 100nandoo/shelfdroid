package dev.halim.shelfdroid

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.expect.PlatformPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named


class PlaybackService : MediaLibraryService(), KoinComponent {
    private lateinit var mediaSession: MediaLibrarySession
    private val dataStoreManager by inject<DataStoreManager>()
    private val io by inject<CoroutineScope>(qualifier = named("io"))

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        println("PlaybackService onTaskRemoved")
        mediaSession.run {
            val currentPosition = player.currentPosition
            io.launch {
                dataStoreManager.setCurrentPosition(currentPosition)
            }
        }
        pauseAllPlayersAndStopSelf()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        get<PlatformPlayer> { parametersOf(this) }
        mediaSession = get { parametersOf(this) }
        get<MediaNotification.Provider> { parametersOf(this) }
        val sdkInt = android.os.Build.VERSION.SDK_INT
        if(sdkInt < 34){
            setMediaNotificationProvider(get())
        }
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