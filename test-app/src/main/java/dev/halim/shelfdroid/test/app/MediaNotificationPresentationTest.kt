package dev.halim.shelfdroid.test.app

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.ui.navigation.navRequestFromIntent
import dev.halim.shelfdroid.core.ui.navigation.NavRequest
import dev.halim.shelfdroid.helper.Helper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MediaNotificationPresentationTest {
  @get:Rule val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var helper: Helper

  @Test
  fun retainedMiniNotificationTapUsesExpandedAfterPreferenceChange() {
    assertRetainedTapUsesUpdatedPresentation(
      MediaNotificationPlayerPresentation.MiniPlayer,
      MediaNotificationPlayerPresentation.ExpandedPlayer,
    )
  }

  @Test
  fun retainedExpandedNotificationTapUsesMiniAfterPreferenceChange() {
    assertRetainedTapUsesUpdatedPresentation(
      MediaNotificationPlayerPresentation.ExpandedPlayer,
      MediaNotificationPlayerPresentation.MiniPlayer,
    )
  }

  private fun assertRetainedTapUsesUpdatedPresentation(
    initial: MediaNotificationPlayerPresentation,
    updated: MediaNotificationPlayerPresentation,
  ) {
    hiltRule.inject()
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val retainedTap = helper.createOpenPlayerIntent("notification-test-book", context, initial)
    val refreshedTap = helper.createOpenPlayerIntent(
      "notification-test-book", context, updated, MediaNotificationOpeningScreen.MediaDetails,
    )
    try {
      // System media controls can keep the tap token obtained before settings changed.
      for (tap in listOf(refreshedTap, retainedTap)) {
        val delivered = send(tap)
        assertEquals(
          NavRequest.OpenMedia(
            mediaId = "notification-test-book",
            playerPresentation = updated,
            openingScreen = MediaNotificationOpeningScreen.MediaDetails,
          ),
          navRequestFromIntent(
            action = delivered.action,
            mediaId = delivered.getStringExtra(Helper.EXTRA_MEDIA_ID),
            openingScreenName =
              delivered.getStringExtra(Helper.EXTRA_MEDIA_NOTIFICATION_OPENING_SCREEN),
            playerPresentationName =
              delivered.getStringExtra(Helper.EXTRA_MEDIA_NOTIFICATION_PLAYER_PRESENTATION),
          ),
        )
      }
    } finally {
      retainedTap.cancel()
      refreshedTap.cancel()
    }
  }

  private fun send(pendingIntent: PendingIntent): Intent {
    val completed = CountDownLatch(1)
    var delivered: Intent? = null
    pendingIntent.send(0, { _, intent, _, _, _ ->
      delivered = intent
      completed.countDown()
    }, Handler(Looper.getMainLooper()))
    assertTrue("Notification tap was not delivered", completed.await(5, TimeUnit.SECONDS))
    return requireNotNull(delivered)
  }
}
