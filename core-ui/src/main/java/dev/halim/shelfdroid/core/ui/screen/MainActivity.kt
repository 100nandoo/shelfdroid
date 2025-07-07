package dev.halim.shelfdroid.core.ui.screen

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.navigation.MainNavigation
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  companion object {
    const val EXTRA_MEDIA_ID = "media_id"
  }

  @Inject lateinit var settingsRepository: SettingsRepository
  @Inject lateinit var mediaControllerFuture: Lazy<ListenableFuture<MediaController>>
  var mediaController: MediaController? = null
  private var pendingMediaId by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    installSplashScreen()
    handleExtra()
    Log.d("MainActivity", "onCreate called")

    setContent {
      val isDarkMode by settingsRepository.darkMode.collectAsState(true)
      val isDynamic by settingsRepository.dynamicTheme.collectAsState(false)
      val token by
        settingsRepository.token.collectAsState(runBlocking { settingsRepository.token.first() })
      ShelfDroidTheme(darkTheme = isDarkMode, dynamicColor = isDynamic) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            isLoggedIn = token.isBlank().not(),
            pendingMediaId = pendingMediaId,
            onMediaIdHandled = { pendingMediaId = null },
          )
        }
      }
    }
  }

  fun initMediaController() {
    lifecycleScope.launch { runCatching { mediaController = mediaControllerFuture.get().await() } }
  }

  override fun onDestroy() {
    super.onDestroy()
    mediaController?.release()
  }

  private fun handleExtra() {
    val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
    pendingMediaId = mediaId
  }
}
