package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerWidgetThemeTest {
  @Test
  fun `active collectors receive new colors after theme preferences change`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val darkMode = MutableStateFlow(true)
    val dynamicTheme = MutableStateFlow(false)
    val primaryColors = mutableListOf<Color>()
    backgroundScope.launch {
      playerWidgetColorProviders(context, darkMode, dynamicTheme).collect {
        primaryColors += it.primary.getColor(context)
      }
    }
    runCurrent()
    darkMode.value = false
    runCurrent()
    dynamicTheme.value = true
    runCurrent()

    assertEquals(
      listOf(darkScheme.primary, lightScheme.primary, dynamicLightColorScheme(context).primary),
      primaryColors,
    )
  }
}
