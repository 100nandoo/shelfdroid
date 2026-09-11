package dev.halim.shelfdroid.media.service

import com.google.common.collect.ImmutableList
import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaNotificationButtonsTest {
  private val firstChapter = PlayerChapter(id = 1, chapterPosition = ChapterPosition.First)
  private val lastChapter = PlayerChapter(id = 2, chapterPosition = ChapterPosition.Last)

  @Test
  fun `places next chapter after sleep timer when it is available`() {
    val buttons =
      mediaNotificationButtons(
        "Next chapter",
        PlayerUiState(
          playerChapters = listOf(firstChapter, lastChapter),
          currentChapter = firstChapter,
        ),
        isChapterTransitioning = false,
        isSleepTimerActive = false,
      )

    assertEquals(
      listOf(CUSTOM_BACK, CUSTOM_FORWARD, CUSTOM_SLEEP_TIMER, CUSTOM_NEXT_CHAPTER),
      buttons.map { it.sessionCommand?.customAction },
    )
    assertTrue(buttons.last().isEnabled)
  }

  @Test
  fun `keeps next chapter visible but disabled on the final chapter`() {
    val buttons =
      mediaNotificationButtons(
        "Next chapter",
        PlayerUiState(
          playerChapters = listOf(firstChapter, lastChapter),
          currentChapter = lastChapter,
        ),
        isChapterTransitioning = false,
        isSleepTimerActive = true,
      )

    assertEquals(CUSTOM_SLEEP_TIMER, buttons[2].sessionCommand?.customAction)
    assertEquals(CUSTOM_NEXT_CHAPTER, buttons[3].sessionCommand?.customAction)
    assertFalse(buttons[3].isEnabled)
  }

  @Test
  fun `shows previous chapter when it is configured and available`() {
    val buttons =
      mediaNotificationButtons(
        "Next chapter",
        PlayerUiState(
          playerChapters = listOf(firstChapter, lastChapter),
          currentChapter = lastChapter,
        ),
        isChapterTransitioning = false,
        isSleepTimerActive = false,
        notificationPrefs =
          NotificationPrefs(
            firstAction = MediaNotificationAction.PreviousChapter,
            secondAction = MediaNotificationAction.None,
          ),
        previousChapterDisplayName = "Previous chapter",
      )

    assertEquals(CUSTOM_PREVIOUS_CHAPTER, buttons[2].sessionCommand?.customAction)
    assertTrue(buttons[2].isEnabled)
  }

  @Test
  fun `omits next chapter for episodes and single chapter books`() {
    val podcastButtons =
      mediaNotificationButtons(
        "Next chapter",
        PlayerUiState(
          episodeId = "episode-1",
          playerChapters = listOf(firstChapter, lastChapter),
          currentChapter = firstChapter,
        ),
        isChapterTransitioning = false,
        isSleepTimerActive = false,
      )
    val singleChapterButtons =
      mediaNotificationButtons(
        "Next chapter",
        PlayerUiState(playerChapters = listOf(firstChapter), currentChapter = firstChapter),
        isChapterTransitioning = false,
        isSleepTimerActive = false,
      )

    assertFalse(podcastButtons.any { it.sessionCommand?.customAction == CUSTOM_NEXT_CHAPTER })
    assertFalse(singleChapterButtons.any { it.sessionCommand?.customAction == CUSTOM_NEXT_CHAPTER })
  }

  @Test
  fun `uses configured custom action order and omits empty slots`() {
    val buttons =
      mediaNotificationButtons(
        "Next chapter",
        NextChapterControlState(visible = true, enabled = true),
        isSleepTimerActive = false,
        notificationPrefs =
          NotificationPrefs(
            firstAction = MediaNotificationAction.PlaybackSpeed,
            secondAction = MediaNotificationAction.None,
          ),
      )

    assertEquals(
      listOf(CUSTOM_BACK, CUSTOM_FORWARD, CUSTOM_PLAYBACK_SPEED),
      buttons.map { it.sessionCommand?.customAction },
    )
  }

  @Test
  fun `supports two empty notification action slots`() {
    val buttons =
      mediaNotificationButtons(
        "Next chapter",
        NextChapterControlState(visible = true, enabled = true),
        isSleepTimerActive = false,
        notificationPrefs =
          NotificationPrefs(
            firstAction = MediaNotificationAction.None,
            secondAction = MediaNotificationAction.None,
          ),
      )

    assertEquals(
      listOf(CUSTOM_BACK, CUSTOM_FORWARD),
      buttons.map { it.sessionCommand?.customAction },
    )
  }

  @Test
  fun `uses configured seek intervals for notification controls`() {
    val buttons =
      mediaNotificationButtons(
        "Next chapter",
        NextChapterControlState(visible = true, enabled = true),
        isSleepTimerActive = false,
        seekBackSeconds = 60,
        seekForwardSeconds = 15,
      )

    assertEquals("Rewind 60s", buttons[0].displayName)
    assertEquals("Forward 15s", buttons[1].displayName)
  }

  @Test
  fun `uses rounded icon matching selected playback speed`() {
    assertEquals(CoreR.drawable.speed_0_5x, playbackSpeedIconResId(0.5f))
    assertEquals(CoreR.drawable.speed_0_75, playbackSpeedIconResId(0.75f))
    assertEquals(CoreR.drawable.speed, playbackSpeedIconResId(1f))
    assertEquals(CoreR.drawable.speed_1_25, playbackSpeedIconResId(1.25f))
    assertEquals(CoreR.drawable.speed_1_5, playbackSpeedIconResId(1.5f))
    assertEquals(CoreR.drawable.speed_1_75, playbackSpeedIconResId(1.75f))
    assertEquals(CoreR.drawable.speed_2x, playbackSpeedIconResId(2f))
    assertEquals(CoreR.drawable.speed, playbackSpeedIconResId(1.1f))
  }

  @Test
  fun `provider button resolution restores disabled next chapter after Media3 filtering`() {
    val disabledNextChapter = nextChapterCommandButton("Next chapter", isEnabled = false)
    val preferences =
      ImmutableList.of(
        MediaNotificationButtons.BACK_COMMAND_BUTTON,
        MediaNotificationButtons.FORWARD_COMMAND_BUTTON,
        MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON,
        disabledNextChapter,
      )
    val resolved =
      ImmutableList.of(
        MediaNotificationButtons.BACK_COMMAND_BUTTON,
        MediaNotificationButtons.FORWARD_COMMAND_BUTTON,
        MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON,
      )

    val notificationButtons = addDisabledNextChapterButton(resolved, preferences)

    assertEquals(CUSTOM_NEXT_CHAPTER, notificationButtons.last().sessionCommand?.customAction)
    assertFalse(notificationButtons.last().isEnabled)
  }

  @Test
  fun `restores disabled next chapter in configured custom action order`() {
    val disabledNextChapter = nextChapterCommandButton("Next chapter", isEnabled = false)
    val preferences =
      ImmutableList.of(
        MediaNotificationButtons.BACK_COMMAND_BUTTON,
        MediaNotificationButtons.FORWARD_COMMAND_BUTTON,
        disabledNextChapter,
        MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON,
      )
    val resolved =
      ImmutableList.of(
        MediaNotificationButtons.BACK_COMMAND_BUTTON,
        MediaNotificationButtons.FORWARD_COMMAND_BUTTON,
        MediaNotificationButtons.SLEEP_TIMER_OFF_BUTTON,
      )

    val notificationButtons = addDisabledNextChapterButton(resolved, preferences)

    assertEquals(CUSTOM_NEXT_CHAPTER, notificationButtons[2].sessionCommand?.customAction)
    assertEquals(CUSTOM_SLEEP_TIMER, notificationButtons[3].sessionCommand?.customAction)
    assertFalse(notificationButtons[2].isEnabled)
  }

  @Test
  fun `restores disabled previous chapter after Media3 filtering`() {
    val disabledPreviousChapter = previousChapterCommandButton("Previous chapter", isEnabled = false)
    val preferences =
      ImmutableList.of(
        MediaNotificationButtons.BACK_COMMAND_BUTTON,
        MediaNotificationButtons.FORWARD_COMMAND_BUTTON,
        disabledPreviousChapter,
      )
    val resolved =
      ImmutableList.of(
        MediaNotificationButtons.BACK_COMMAND_BUTTON,
        MediaNotificationButtons.FORWARD_COMMAND_BUTTON,
      )

    val notificationButtons = addDisabledChapterButtons(resolved, preferences)

    assertEquals(CUSTOM_PREVIOUS_CHAPTER, notificationButtons.last().sessionCommand?.customAction)
    assertFalse(notificationButtons.last().isEnabled)
  }
}
