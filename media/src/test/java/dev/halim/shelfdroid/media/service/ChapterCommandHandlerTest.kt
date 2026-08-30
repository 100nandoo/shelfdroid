package dev.halim.shelfdroid.media.service

import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterCommandHandlerTest {
  private val handler = ChapterCommandHandler()
  private val chapters =
    listOf(
      chapter(0, ChapterPosition.First),
      chapter(1, ChapterPosition.Middle),
      chapter(2, ChapterPosition.Last),
    )

  @Test
  fun `previous restarts current chapter after three seconds`() {
    val state = bookState(chapters, currentIndex = 1)

    val decision = handler.resolve(ChapterCommand.Previous, state, 3_001)

    assertEquals(ChapterCommandDecision.Restart, decision)
  }

  @Test
  fun `previous selects preceding chapter at three seconds`() {
    val state = bookState(chapters, currentIndex = 1)

    val decision = handler.resolve(ChapterCommand.Previous, state, 3_000)

    assertEquals(ChapterCommandDecision.ChangeChapter(0), decision)
  }

  @Test
  fun `previous selects preceding chapter before three seconds`() {
    val state = bookState(chapters, currentIndex = 2)

    val decision = handler.resolve(ChapterCommand.Previous, state, 2_999)

    assertEquals(ChapterCommandDecision.ChangeChapter(1), decision)
  }

  @Test
  fun `previous restarts first chapter`() {
    val state = bookState(chapters, currentIndex = 0)

    val decision = handler.resolve(ChapterCommand.Previous, state, 0)

    assertEquals(ChapterCommandDecision.Restart, decision)
  }

  @Test
  fun `book without chapters restarts previous and disables next`() {
    val state = bookState(emptyList(), currentIndex = null)

    val availability = handler.availability(state)

    assertTrue(availability.previousEnabled)
    assertFalse(availability.nextEnabled)
    assertEquals(
      ChapterCommandDecision.Restart,
      handler.resolve(ChapterCommand.Previous, state, 0),
    )
    assertEquals(
      ChapterCommandDecision.Unavailable,
      handler.resolve(ChapterCommand.Next, state, 0),
    )
  }

  @Test
  fun `single chapter book restarts previous and disables next`() {
    val singleChapter = listOf(chapter(0, ChapterPosition.First))
    val state = bookState(singleChapter, currentIndex = 0)

    val availability = handler.availability(state)

    assertTrue(availability.previousEnabled)
    assertFalse(availability.nextEnabled)
    assertEquals(
      ChapterCommandDecision.Restart,
      handler.resolve(ChapterCommand.Previous, state, 0),
    )
    assertEquals(
      ChapterCommandDecision.Unavailable,
      handler.resolve(ChapterCommand.Next, state, 0),
    )
  }

  @Test
  fun `podcast episode restarts previous and disables next`() {
    val state = PlayerUiState(id = "podcast", episodeId = "episode", currentChapter = null)

    val availability = handler.availability(state)

    assertTrue(availability.previousEnabled)
    assertFalse(availability.nextEnabled)
    assertEquals(
      ChapterCommandDecision.Restart,
      handler.resolve(ChapterCommand.Previous, state, 0),
    )
    assertEquals(
      ChapterCommandDecision.Unavailable,
      handler.resolve(ChapterCommand.Next, state, 0),
    )
  }

  @Test
  fun `next selects valid following chapter`() {
    val state = bookState(chapters, currentIndex = 1)

    val availability = handler.availability(state)

    assertTrue(availability.nextEnabled)
    assertEquals(
      ChapterCommandDecision.ChangeChapter(2),
      handler.resolve(ChapterCommand.Next, state, 0),
    )
  }

  @Test
  fun `final chapter disables and rejects next`() {
    val state = bookState(chapters, currentIndex = 2)

    val availability = handler.availability(state)

    assertFalse(availability.nextEnabled)
    assertEquals(
      ChapterCommandDecision.Unavailable,
      handler.resolve(ChapterCommand.Next, state, 0),
    )
  }

  @Test
  fun `commands are unavailable without current playback`() {
    val state = PlayerUiState(id = "", currentChapter = null)

    val availability = handler.availability(state)

    assertFalse(availability.previousEnabled)
    assertFalse(availability.nextEnabled)
    assertEquals(
      ChapterCommandDecision.Unavailable,
      handler.resolve(ChapterCommand.Previous, state, 0),
    )
  }

  private fun bookState(chapters: List<PlayerChapter>, currentIndex: Int?): PlayerUiState =
    PlayerUiState(
      id = "book",
      playerChapters = chapters,
      currentChapter = currentIndex?.let(chapters::get),
    )

  private fun chapter(id: Int, position: ChapterPosition) =
    PlayerChapter(id = id, title = "Chapter $id", chapterPosition = position)
}
