package dev.halim.shelfdroid.core.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.MainNavigation
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var settingsRepository: SettingsRepository
  @Inject lateinit var mediaControllerFuture: Lazy<ListenableFuture<MediaController>>
  var mediaController: MediaController? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    installSplashScreen()

    setContent {
      val isDarkMode by settingsRepository.darkMode.collectAsState(true)
      val isDynamic by settingsRepository.dynamicTheme.collectAsState(false)
      val token by
        settingsRepository.token.collectAsState(runBlocking { settingsRepository.token.first() })
      ShelfDroidTheme(darkTheme = isDarkMode, dynamicColor = isDynamic) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(token.isBlank().not())
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
}
