package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.glance.material3.ColorProviders
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme
import dev.halim.shelfdroid.widget.player.playerWidgetDataStoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal fun playerWidgetColorProviders(context: Context) =
  playerWidgetDataStoreManager(context).let { dataStoreManager ->
    playerWidgetColorProviders(context, dataStoreManager.darkMode, dataStoreManager.dynamicTheme)
  }

internal fun playerWidgetColorProviders(
  context: Context,
  darkMode: Flow<Boolean>,
  dynamicTheme: Flow<Boolean>,
) =
  combine(darkMode, dynamicTheme) { dark, dynamic ->
    val colorScheme = playerWidgetColorScheme(context, dark, dynamic)
    ColorProviders(light = colorScheme, dark = colorScheme)
  }

private fun playerWidgetColorScheme(
  context: Context,
  darkMode: Boolean,
  dynamicTheme: Boolean,
): ColorScheme =
  when {
    dynamicTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
      if (darkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    darkMode -> darkScheme
    else -> lightScheme
  }
