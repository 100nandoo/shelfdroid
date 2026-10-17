package dev.halim.shelfdroid.widget.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerWidgetUpdatesTest {
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `theme preference changes update widgets while playback stays the same`() = runTest {
    val playbackData = MutableStateFlow<PlayerWidgetData?>(null)
    val darkMode = MutableStateFlow(true)
    val dynamicTheme = MutableStateFlow(false)
    val updates = mutableListOf<Unit>()

    backgroundScope.launch {
      playerWidgetUpdateFlow(playbackData, darkMode, dynamicTheme).collect(updates::add)
    }
    runCurrent()

    darkMode.value = false
    runCurrent()
    dynamicTheme.value = true
    runCurrent()

    assertEquals(3, updates.size)
  }
}
