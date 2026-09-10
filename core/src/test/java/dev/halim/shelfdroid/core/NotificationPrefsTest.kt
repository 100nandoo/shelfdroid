package dev.halim.shelfdroid.core

import dev.halim.shelfdroid.core.playback.DEFAULT_PLAYBACK_SPEED_CYCLE
import dev.halim.shelfdroid.core.playback.nextPlaybackSpeed
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPrefsTest {

  @Test
  fun defaultsUseTimerThenNextChapterAndCommonSpeedCycle() {
    val prefs = NotificationPrefs()

    assertEquals(MediaNotificationAction.SleepTimer, prefs.firstAction)
    assertEquals(MediaNotificationAction.NextChapter, prefs.secondAction)
    assertEquals(DEFAULT_PLAYBACK_SPEED_CYCLE, prefs.playbackSpeedCycle)
  }

  @Test
  fun normalizePlaybackSpeedCycleKeepsOnlyPresetValuesInAscendingOrder() {
    assertEquals(
      listOf(0.75f, 1.25f, 2f),
      normalizePlaybackSpeedCycle(listOf(2f, 1.25f, 1.25f, 0.75f, 1.1f)),
    )
  }

  @Test
  fun normalizePlaybackSpeedCycleFallsBackWhenFewerThanTwoValuesRemain() {
    assertEquals(DEFAULT_PLAYBACK_SPEED_CYCLE, normalizePlaybackSpeedCycle(listOf(1.25f)))
  }

  @Test
  fun nextPlaybackSpeedChoosesClosestHigherSpeedAndWraps() {
    val selected = listOf(1f, 1.5f, 2f)

    assertEquals(1.5f, nextPlaybackSpeed(1.25f, selected))
    assertEquals(1f, nextPlaybackSpeed(2f, selected))
    assertEquals(1f, nextPlaybackSpeed(0.5f, selected))
  }

  @Test
  fun notificationPrefsRoundTripThroughDataStoreJson() {
    val prefs =
      NotificationPrefs(
        firstAction = MediaNotificationAction.PlaybackSpeed,
        secondAction = MediaNotificationAction.None,
        playbackSpeedCycle = listOf(0.5f, 1.5f),
      )

    val encoded = Json.encodeToString(NotificationPrefs.serializer(), prefs)
    assertEquals(prefs, Json.decodeFromString(NotificationPrefs.serializer(), encoded))
  }
}
