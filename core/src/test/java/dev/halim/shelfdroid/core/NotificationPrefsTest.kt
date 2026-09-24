package dev.halim.shelfdroid.core

import dev.halim.shelfdroid.core.playback.DEFAULT_PLAYBACK_SPEED_CYCLE
import dev.halim.shelfdroid.core.playback.nextPlaybackSpeed
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.playback.togglePlaybackSpeed
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
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
  fun notificationTapDefaultsToHomeWithExpandedPlayer() {
    val prefs = NotificationPrefs()

    assertEquals(MediaNotificationOpeningScreen.Home, prefs.openingScreen)
    assertEquals(MediaNotificationPlayerPresentation.ExpandedPlayer, prefs.playerPresentation)
  }

  @Test
  fun legacyNotificationPrefsKeepToggleModeAndInitialOneFiveCycle() {
    val prefs =
      Json.decodeFromString(NotificationPrefs.serializer(), """{"sleepTimerMinutes":30}""")

    assertEquals(30, prefs.sleepTimerMinutes)
    assertEquals(SleepTimerNotificationMode.Toggle, prefs.sleepTimerMode)
    assertEquals(listOf(1, 5), prefs.sleepTimerCycle)
  }

  @Test
  fun legacyNotificationPrefsKeepPlayerPresentationAndDefaultOpeningScreenToHome() {
    val prefs =
      Json.decodeFromString(
        NotificationPrefs.serializer(),
        """{"tapDestination":"MiniPlayer"}""",
      )

    assertEquals(MediaNotificationPlayerPresentation.MiniPlayer, prefs.playerPresentation)
    assertEquals(MediaNotificationOpeningScreen.Home, prefs.openingScreen)
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
        playerPresentation = MediaNotificationPlayerPresentation.MiniPlayer,
        openingScreen = MediaNotificationOpeningScreen.MediaDetails,
        playbackSpeedCycle = listOf(0.5f, 1.5f),
        sleepTimerMode = SleepTimerNotificationMode.Cyclical,
        sleepTimerCycle = listOf(5, 30),
      )

    val encoded = Json.encodeToString(NotificationPrefs.serializer(), prefs)
    assertEquals(prefs, Json.decodeFromString(NotificationPrefs.serializer(), encoded))
  }

  @Test
  fun legacyNotificationPrefsDefaultToCyclicalPlaybackSpeedMode() {
    val prefs = Json.decodeFromString(NotificationPrefs.serializer(), "{\"sleepTimerMinutes\":30}")

    assertEquals("Cyclical", prefs.playbackSpeedMode.name)
    assertEquals(1.5f, prefs.playbackSpeedToggleTarget)
  }

  @Test
  fun togglePlaybackSpeedUsesOneXAsBaseline() {
    assertEquals(1.5f, togglePlaybackSpeed(1f, 1.5f))
    assertEquals(1f, togglePlaybackSpeed(1.5f, 1.5f))
    assertEquals(1.5f, togglePlaybackSpeed(1.25f, 1.5f))
    assertEquals(1f, togglePlaybackSpeed(1.5005f, 1.5f))
  }

  @Test
  fun togglePlaybackSpeedRejectsOneXAndUnsupportedTargets() {
    assertEquals(1.5f, togglePlaybackSpeed(1f, 1f))
    assertEquals(1.5f, togglePlaybackSpeed(1f, 1.1f))
  }
}
