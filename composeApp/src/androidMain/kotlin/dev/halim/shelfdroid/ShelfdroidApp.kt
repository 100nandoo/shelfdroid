package dev.halim.shelfdroid

import android.app.Application
import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.halim.shelfdroid.expect.initializeKoin
import org.koin.android.ext.koin.androidContext

class ShelfdroidApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeKoin {
            androidContext(this@ShelfdroidApp)
        }
        startPlaybackService()
    }

    private fun startPlaybackService() {
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        MediaController.Builder(this, sessionToken).buildAsync()
    }
}