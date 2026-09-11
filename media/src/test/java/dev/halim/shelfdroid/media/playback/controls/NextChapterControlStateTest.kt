package dev.halim.shelfdroid.media.playback.controls

import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextChapterControlStateTest {
  private val firstChapter = PlayerChapter(id = 1, chapterPosition = ChapterPosition.First)
  private val middleChapter = PlayerChapter(id = 2, chapterPosition = ChapterPosition.Middle)
  private val lastChapter = PlayerChapter(id = 3, chapterPosition = ChapterPosition.Last)

  @Test
  fun `shows enabled control for a book with a next chapter`() {
    val state =
      nextChapterControlState(
        PlayerUiState(
          playerChapters = listOf(firstChapter, middleChapter, lastChapter),
          currentChapter = middleChapter,
        )
      )

    assertTrue(state.visible)
    assertTrue(state.enabled)
  }

  @Test
  fun `shows disabled control on the final chapter`() {
    val state =
      nextChapterControlState(
        PlayerUiState(
          playerChapters = listOf(firstChapter, lastChapter),
          currentChapter = lastChapter,
        )
      )

    assertTrue(state.visible)
    assertFalse(state.enabled)
  }

  @Test
  fun `hides control for podcasts and books without a next chapter`() {
    val podcastState =
      nextChapterControlState(
        PlayerUiState(
          episodeId = "episode-1",
          playerChapters = listOf(firstChapter, lastChapter),
          currentChapter = firstChapter,
        )
      )
    val singleChapterState =
      nextChapterControlState(
        PlayerUiState(playerChapters = listOf(firstChapter), currentChapter = firstChapter)
      )

    assertFalse(podcastState.visible)
    assertFalse(podcastState.enabled)
    assertFalse(singleChapterState.visible)
    assertFalse(singleChapterState.enabled)
  }

  @Test
  fun `disables control while a chapter transition is in progress`() {
    val state =
      nextChapterControlState(
        PlayerUiState(
          playerChapters = listOf(firstChapter, middleChapter),
          currentChapter = firstChapter,
        ),
        isTransitioning = true,
      )

    assertTrue(state.visible)
    assertFalse(state.enabled)
  }
}
