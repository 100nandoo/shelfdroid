package dev.halim.shelfdroid.widget

import dev.halim.shelfdroid.media.presentation.PlaybackPresentationObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackWidgetSynchronizationTest {

  @Test
  fun playbackPresentationObserver_requestsAnAllInstanceRefresh() = runTest {
    val requester = RecordingRefreshRequester()
    val observer = PlaybackWidgetPresentationObserver(requester)

    observer.onPlaybackPresentationChanged()

    assertEquals(1, requester.requests)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun lightDarkAndDynamicColorChanges_notifyThePresentationObserver() = runTest {
    val preferences =
      MutableStateFlow(
        PlaybackWidgetThemePreferences(isDark = false, useDynamicColor = false)
      )
    var notifications = 0
    val observer = PlaybackPresentationObserver { notifications++ }
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      observePlaybackWidgetThemeChanges(preferences, observer)
    }

    assertEquals(1, notifications)
    preferences.value = preferences.value.copy(isDark = true)
    preferences.value = preferences.value.copy(useDynamicColor = true)

    assertEquals(3, notifications)
  }

  private class RecordingRefreshRequester : PlaybackWidgetRefreshRequester {
    var requests = 0

    override suspend fun requestAllInstancesRefresh() {
      requests++
    }
  }
}
