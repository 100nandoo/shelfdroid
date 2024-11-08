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
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.expect.PlayerWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val mediaManager: MediaManager by inject()
    private val coroutineScope: CoroutineScope by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextUtils.context = applicationContext
        enableEdgeToEdge()
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        setContent {
            val isDarkMode = SharedObject.isDarkMode.collectAsState()
            controller.isAppearanceLightStatusBars = isDarkMode.value.not()
            controller.isAppearanceLightNavigationBars = isDarkMode.value.not()
            App()
        }
    }

    override fun onStart() {
        Log.d("Main", "onStart")
        super.onStart()
        initializeMediaController()
    }

    private fun initializeMediaController() {
        Log.d("Main", "initializeMediaController")
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get()
                    mediaController = controller
                    controller?.let {
                        SharedObject.playerWrapper = PlayerWrapper(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        Log.d("Main", "onStop")
        super.onStop()
        coroutineScope.cancel()
        releaseMediaController()
    }

    private fun releaseMediaController() {
        Log.d("Main", "releaseMediaController")
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaManager.release()
        mediaController?.release()
        mediaController = null
        controllerFuture = null
        SharedObject.playerWrapper = null
    }

    override fun onDestroy() {
        Log.d("Main", "onDestroy")
        releaseMediaController()
        coroutineScope.cancel()
        super.onDestroy()
    }
}