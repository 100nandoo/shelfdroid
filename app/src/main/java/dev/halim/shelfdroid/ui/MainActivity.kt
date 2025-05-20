package dev.halim.shelfdroid.ui

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
import dagger.hilt.android.AndroidEntryPoint
import dev.halim.shelfdroid.core.data.settings.SettingsRepository
import dev.halim.shelfdroid.core.ui.theme.ShelfDroidTheme
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var settingsRepository: SettingsRepository

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
}
