package dev.halim.shelfdroid.core.ui.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.data.Helper.Companion.ACTION_OPEN_PLAYER
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.navigation.MainNavigation
import dev.halim.shelfdroid.core.ui.navigation.NavRequest
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme
import dev.halim.shelfdroid.media.di.MediaControllerManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  companion object {
    const val EXTRA_MEDIA_ID = "media_id"
  }

  @Inject lateinit var settingsRepository: SettingsRepository
  @Inject lateinit var mediaControllerManager: Lazy<MediaControllerManager>
  private var navRequest by mutableStateOf<NavRequest>(NavRequest())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    installSplashScreen()
    handleExtra()
    Log.d("MainActivity", "onCreate called")

    setContent {
      val isDarkMode by settingsRepository.darkMode.collectAsStateWithLifecycle(true)
      val isDynamic by settingsRepository.dynamicTheme.collectAsStateWithLifecycle(false)
      val token by
        settingsRepository.token.collectAsStateWithLifecycle(
          runBlocking { settingsRepository.token.first() }
        )
      ShelfDroidTheme(darkTheme = isDarkMode, dynamicColor = isDynamic) {
        Surface(
          modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
          color = MaterialTheme.colorScheme.background,
        ) {
          MainNavigation(
            isLoggedIn = token.isBlank().not(),
            navRequest = navRequest,
            onNavRequestComplete = { navRequest = NavRequest() },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleExtra()
  }

  fun initMediaController() {
    mediaControllerManager.get().init(lifecycleScope)
  }

  override fun onDestroy() {
    super.onDestroy()
    mediaControllerManager.get().release()
  }

  private fun handleExtra() {
    val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
    val isOpenPlayer = intent.action == ACTION_OPEN_PLAYER
    navRequest = NavRequest(mediaId = mediaId, isOpenPlayer = isOpenPlayer)
  }
}
