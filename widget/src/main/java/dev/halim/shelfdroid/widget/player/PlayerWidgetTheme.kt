package dev.halim.shelfdroid.widget.player

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.material3.ColorProviders
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme
import kotlinx.coroutines.flow.first

internal suspend fun playerWidgetColorProviders(context: Context) =
  playerWidgetColorScheme(context).let { colorScheme ->
    ColorProviders(light = colorScheme, dark = colorScheme)
  }

private suspend fun playerWidgetColorScheme(context: Context): ColorScheme {
  val dataStoreManager = playerWidgetDataStoreManager(context)
  val darkMode = dataStoreManager.darkMode.first()
  val dynamicTheme = dataStoreManager.dynamicTheme.first()

  return when {
    dynamicTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
      if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    darkMode -> darkScheme
    else -> lightScheme
  }
}
