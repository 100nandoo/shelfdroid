package dev.halim.shelfdroid.widget

import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.PlayerUiState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerWidgetContentTest {
  @Test
  fun `pause and player visibility preserve metadata without redundant updates`() = runTest {
    val current =
      PlayerUiState(
        id = "book",
        title = "Chapter 2",
        author = "Book author",
        state = PlayerState.Big,
        playPause = PlayPauseControlState(isPlaying = true),
      )

    val content =
      flowOf(
          current,
          current.copy(currentTime = 10.0),
          current.copy(playPause = PlayPauseControlState(isPlaying = false)),
          current.copy(state = PlayerState.Small),
          current.copy(state = PlayerState.TempHidden),
          current.copy(state = PlayerState.Hidden()),
          current.copy(title = "Chapter 3"),
          PlayerUiState(),
        )
        .widgetContent()
        .toList()

    assertEquals(3, content.size)
    assertEquals("Chapter 2", content[0]?.title)
    assertEquals("Book author", content[0]?.author)
    assertEquals("Chapter 3", content[1]?.title)
    assertNull(content[2])
  }

  @Test
  fun `podcast metadata matches player and clears only when media identity is absent`() {
    val episode =
      PlayerUiState(
        id = "podcast",
        episodeId = "episode",
        title = "Episode title",
        author = "Podcast creator",
      )

    assertEquals("Episode title", episode.toWidgetContent()?.title)
    assertEquals("Podcast creator", episode.toWidgetContent()?.author)
    assertNull(episode.copy(id = "").toWidgetContent())
    assertEquals("", episode.copy(title = "").toWidgetContent()?.title)
  }
}
