package dev.halim.shelfdroid.media.presentation

import androidx.media3.common.Player
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackPresentationObserverTest {

  @Test
  fun zeroObservers_acceptsPresentationNotifications() = runTest {
    PlaybackPresentationNotifier(emptySet()).notifyPresentationChanged()
  }

  @Test
  fun multipleObservers_eachReceiveTheSamePresentationNotification() = runTest {
    val notifications = mutableListOf<String>()
    val notifier =
      PlaybackPresentationNotifier(
        setOf(
          PlaybackPresentationObserver { notifications += "first" },
          PlaybackPresentationObserver { notifications += "second" },
        )
      )

    notifier.notifyPresentationChanged()

    assertEquals(setOf("first", "second"), notifications.toSet())
  }

  @Test
  fun failingObserver_doesNotPreventAnotherObserver() = runTest {
    var healthyObserverNotifications = 0
    val notifier =
      PlaybackPresentationNotifier(
        linkedSetOf(
          PlaybackPresentationObserver { error("observer failure") },
          PlaybackPresentationObserver { healthyObserverNotifications++ },
        )
      )

    notifier.notifyPresentationChanged()

    assertEquals(1, healthyObserverNotifications)
  }

  @Test
  fun visiblePlaybackMediaMetadataIntentErrorAndAvailabilityEvents_areIncluded() {
    assertTrue(Player.EVENT_MEDIA_ITEM_TRANSITION in PRESENTATION_VISIBLE_PLAYER_EVENTS)
    assertTrue(Player.EVENT_TIMELINE_CHANGED in PRESENTATION_VISIBLE_PLAYER_EVENTS)
    assertTrue(Player.EVENT_MEDIA_METADATA_CHANGED in PRESENTATION_VISIBLE_PLAYER_EVENTS)
    assertTrue(Player.EVENT_PLAYBACK_STATE_CHANGED in PRESENTATION_VISIBLE_PLAYER_EVENTS)
    assertTrue(Player.EVENT_PLAY_WHEN_READY_CHANGED in PRESENTATION_VISIBLE_PLAYER_EVENTS)
    assertTrue(Player.EVENT_PLAYER_ERROR in PRESENTATION_VISIBLE_PLAYER_EVENTS)
    assertTrue(Player.EVENT_AVAILABLE_COMMANDS_CHANGED in PRESENTATION_VISIBLE_PLAYER_EVENTS)
  }

  @Test
  fun positionOnlyEvents_areExcludedFromPresentationNotifications() {
    assertFalse(Player.EVENT_POSITION_DISCONTINUITY in PRESENTATION_VISIBLE_PLAYER_EVENTS)
  }
}
