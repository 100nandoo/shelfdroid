package dev.halim.shelfdroid

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.expect.initializeKoin
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {

    private val dataStoreManager: DataStoreManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initializeKoin {
            androidContext(applicationContext)
        }
        setContent {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val isDarkMode = dataStoreManager.isDarkMode.collectAsState(true)
            controller.isAppearanceLightStatusBars = isDarkMode.value.not()
            controller.isAppearanceLightNavigationBars = isDarkMode.value.not()
            App()
        }
        startPlaybackService()
    }

    private fun startPlaybackService() {
        val sessionToken = SessionToken(
            this,
            ComponentName(this, PlaybackService::class.java)
        )
        MediaController.Builder(this, sessionToken).buildAsync()
    }
}