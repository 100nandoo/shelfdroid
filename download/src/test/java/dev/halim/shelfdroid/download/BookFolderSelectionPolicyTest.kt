package dev.halim.shelfdroid.download

import org.junit.Assert.assertEquals
import org.junit.Test

class BookFolderSelectionPolicyTest {
  private val policy = BookFolderSelectionPolicy()

  @Test
  fun `keeps exact relative path when current folder already has tracks`() {
    val resolved =
      policy.resolveRelativePath(
        exactRelativePath = "Download/ShelfDroid/books/Dune_Frank Herbert/",
        filenames = listOf("01.mp3", "02.mp3"),
        matches =
          listOf(
            BookFolderMatch("Download/ShelfDroid/books/Dune_Frank Herbert/", "01.mp3"),
          ),
      )

    assertEquals("Download/ShelfDroid/books/Dune_Frank Herbert/", resolved)
  }

  @Test
  fun `reuses previous folder when metadata changed but track set still matches`() {
    val resolved =
      policy.resolveRelativePath(
        exactRelativePath = "Download/ShelfDroid/books/Dune Messiah_Frank Herbert/",
        filenames = listOf("01.mp3", "02.mp3"),
        matches =
          listOf(
            BookFolderMatch("Download/ShelfDroid/books/Dune_Frank Herbert/", "01.mp3"),
            BookFolderMatch("Download/ShelfDroid/books/Dune_Frank Herbert/", "02.mp3"),
          ),
      )

    assertEquals("Download/ShelfDroid/books/Dune_Frank Herbert/", resolved)
  }

  @Test
  fun `falls back to current metadata path when matches are ambiguous`() {
    val resolved =
      policy.resolveRelativePath(
        exactRelativePath = "Download/ShelfDroid/books/Dune Messiah_Frank Herbert/",
        filenames = listOf("01.mp3"),
        matches =
          listOf(
            BookFolderMatch("Download/ShelfDroid/books/Dune_Frank Herbert/", "01.mp3"),
            BookFolderMatch("Download/ShelfDroid/books/Children of Dune_Frank Herbert/", "01.mp3"),
          ),
      )

    assertEquals("Download/ShelfDroid/books/Dune Messiah_Frank Herbert/", resolved)
  }
}
