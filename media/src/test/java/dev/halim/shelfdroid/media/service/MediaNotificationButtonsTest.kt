package dev.halim.shelfdroid.media.service

import com.google.common.collect.ImmutableList
import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerUiState
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
}
