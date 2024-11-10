package dev.halim.shelfdroid

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dev.halim.shelfdroid.datastore.DataStoreManager
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataStoreManager: DataStoreManager by inject()
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")

        enableEdgeToEdge()
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
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({}, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::controllerFuture.isInitialized) {
            if (!controllerFuture.isDone) {
                controllerFuture.cancel(true)
            } else {
                controllerFuture.get().release()
            }
        }
    }
}