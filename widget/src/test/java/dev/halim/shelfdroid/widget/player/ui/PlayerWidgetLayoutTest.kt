package dev.halim.shelfdroid.widget.player.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerWidgetLayoutTest {
  @Test
  fun `compact spec contains cover visibility and title line decisions`() {
    val spec =
      resolvePlayerWidgetLayout(
        width = 219.dp,
        height = 79.dp,
        fontScale = 1f,
        hasChapterTitle = false,
        showChapterNavigation = true,
      )

    assertEquals(
      PlayerWidgetLayoutSpec.Compact(
        showCover = false,
        coverSize = 48.dp,
        titleMaxLines = 1,
        verticalPadding = 4.dp,
        controlButtonSize = 48.dp,
      ),
      spec,
    )
  }

  @Test
  fun `medium spec accounts for font scale and control count`() {
    val spec =
      resolvePlayerWidgetLayout(
        width = 220.dp,
        height = 164.dp,
        fontScale = 2f,
        hasChapterTitle = true,
        showChapterNavigation = true,
      )

    assertEquals(
      PlayerWidgetLayoutSpec.Medium(
        coverSize = 72.dp,
        titleMaxLines = 1,
        chapterTitleMaxLines = 1,
        showChapterNavigation = true,
        contentSpacing = 4.dp,
        verticalPadding = 8.dp,
        controlButtonSize = 39.2.dp,
      ),
      spec,
    )
  }

  @Test
  fun `large cover changes from side to stacked at its height threshold`() {
    val sideCoverSpec =
      resolvePlayerWidgetLayout(
        width = 320.dp,
        height = 301.dp,
        fontScale = 1f,
        hasChapterTitle = true,
        showChapterNavigation = false,
      )
    val stackedCoverSpec =
      resolvePlayerWidgetLayout(
        width = 320.dp,
        height = 302.dp,
        fontScale = 1f,
        hasChapterTitle = true,
        showChapterNavigation = false,
      )

    assertTrue((sideCoverSpec as PlayerWidgetLayoutSpec.Large).sideCover)
    assertFalse((stackedCoverSpec as PlayerWidgetLayoutSpec.Large).sideCover)
    assertEquals(96.dp, stackedCoverSpec.coverSize)
    assertEquals(2, stackedCoverSpec.headlineMaxLines)
  }
}
