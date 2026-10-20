package dev.halim.shelfdroid.widget.player

import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.SeekControlsState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerWidgetContentTest {
  @Test
  fun `cover changes refresh widget even when title and author stay the same`() = runTest {
    val current = PlayerUiState(id = "book", title = "Book", author = "Author", cover = "cover-a")

    val content =
      flowOf(
          current,
          current.copy(currentTime = 10.0),
          current.copy(cover = "cover-b"),
          current.copy(cover = ""),
          PlayerUiState(),
        )
        .playerWidgetData()
        .toList()

    assertEquals(listOf("cover-a", "cover-b", "", null), content.map { it?.cover })
  }

  @Test
  fun `playback controls refresh while player visibility preserves metadata`() = runTest {
    val current =
      PlayerUiState(
        id = "book",
        title = "Chapter 2",
        author = "Book author",
        state = PlayerState.Big,
        playPause = PlayPauseControlState(isPlaying = true, enabled = true, showPlayIcon = false),
        seekControls = SeekControlsState(seekBackEnabled = true, seekForwardEnabled = true),
      )
    val paused =
      current.copy(
        playPause = PlayPauseControlState(isPlaying = false, enabled = true, showPlayIcon = true)
      )
    val seekDisabled = paused.copy(seekControls = SeekControlsState(seekForwardEnabled = true))

    val content =
      flowOf(
          current,
          current.copy(currentTime = 10.0),
          paused,
          seekDisabled,
          seekDisabled.copy(state = PlayerState.Small),
          seekDisabled.copy(state = PlayerState.TempHidden),
          seekDisabled.copy(state = PlayerState.Hidden()),
          seekDisabled.copy(title = "Chapter 3"),
          PlayerUiState(),
        )
        .playerWidgetData()
        .toList()

    assertEquals(5, content.size)
    assertEquals("Chapter 2", content[0]?.title)
    assertEquals("Book author", content[0]?.author)
    assertEquals(false, content[0]?.showPlayIcon)
    assertEquals(true, content[0]?.playPauseEnabled)
    assertEquals(true, content[0]?.seekBackEnabled)
    assertEquals(true, content[1]?.showPlayIcon)
    assertEquals(false, content[2]?.seekBackEnabled)
    assertEquals("Chapter 3", content[3]?.title)
    assertNull(content[4])
  }

  @Test
  fun `podcast metadata matches player and clears only when media identity is absent`() {
    val episode =
      PlayerUiState(
        id = "podcast",
        episodeId = "episode",
        title = "Episode title",
        author = "Podcast creator",
        libraryItemTitle = "Podcast name",
      )

    assertEquals("Episode title", episode.toPlayerWidgetData()?.title)
    assertEquals("Podcast name", episode.toPlayerWidgetData()?.author)
    assertNull(episode.copy(id = "").toPlayerWidgetData())
    assertEquals("", episode.copy(title = "").toPlayerWidgetData()?.title)
  }

  @Test
  fun `book widget metadata keeps book title separate from current chapter`() {
    val book =
      PlayerUiState(
        id = "book",
        title = "Chapter title",
        author = "Book author",
        libraryItemTitle = "Book title",
        playerChapters = listOf(PlayerChapter(id = 1, title = "Chapter title")),
        currentChapter = PlayerChapter(id = 1, title = "Chapter title"),
      )

    val content = book.toPlayerWidgetData()!!

    assertEquals("Book title", content.title)
    assertEquals("Book author", content.author)
    assertEquals("Chapter title", content.chapterTitle)
  }

  @Test
  fun `chapter metadata and navigation state are available for podcast episodes`() {
    val chapters =
      listOf(
        PlayerChapter(id = 1, title = "First", chapterPosition = ChapterPosition.First),
        PlayerChapter(id = 2, title = "Middle", chapterPosition = ChapterPosition.Middle),
        PlayerChapter(id = 3, title = "Last", chapterPosition = ChapterPosition.Last),
      )
    val episode =
      PlayerUiState(
        id = "podcast",
        episodeId = "episode",
        episodeChapters = chapters,
        currentEpisodeChapter = chapters[1],
      )

    val content = episode.toPlayerWidgetData()!!

    assertEquals("Middle", content.chapterTitle)
    assertTrue(content.showChapterNavigation)
    assertTrue(content.previousChapterEnabled)
    assertTrue(content.nextChapterEnabled)
    assertFalse(
      episode
        .copy(currentEpisodeChapter = chapters.first())
        .toPlayerWidgetData()!!
        .previousChapterEnabled
    )
    assertFalse(
      episode
        .copy(currentEpisodeChapter = chapters.last())
        .toPlayerWidgetData()!!
        .nextChapterEnabled
    )
    assertFalse(
      episode
        .copy(
          episodeChapters = chapters.take(1),
          currentEpisodeChapter = chapters.first(),
        )
        .toPlayerWidgetData()!!
        .showChapterNavigation
    )
  }
}
