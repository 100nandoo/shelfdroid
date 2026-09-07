package dev.halim.shelfdroid.core.ui.screen.home

import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenSortTest {

  @Test
  fun bookFilterAndSort_whenDownloadedOnly_keepsDownloadedBooks() {
    val filtered =
      bookFilterAndSort(
        books =
          listOf(
            BookUiState(id = "downloaded", isDownloaded = true),
            BookUiState(id = "remote"),
          ),
        displayPrefs = DisplayPrefs(filter = Filter.Downloaded),
      )

    assertEquals(listOf("downloaded"), filtered.map(BookUiState::id))
  }

  @Test
  fun bookFilterAndSort_whenAuthorFirstLast_ordersAuthorsCaseInsensitivelyThenTitle() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "zulu", authorFirstLast = "Ada Lovelace", title = "Zulu"),
            book(id = "alpha", authorFirstLast = "ada lovelace", title = "Alpha"),
            book(id = "blank", authorFirstLast = "", title = "Blank"),
            book(id = "bravo", authorFirstLast = "Grace Hopper", title = "Bravo"),
          ),
        displayPrefs = DisplayPrefs(bookSort = BookSort.AuthorFirstLast, sortOrder = SortOrder.Asc),
      )

    assertEquals(listOf("alpha", "zulu", "bravo", "blank"), sorted.map(BookUiState::id))
  }

  @Test
  fun bookFilterAndSort_whenAuthorLastFirstDescending_keepsMissingAuthorsLast() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "smith", authorLastFirst = "Smith, John", title = "Book"),
            book(id = "hopper", authorLastFirst = "Hopper, Grace", title = "Book"),
            book(id = "blank", authorLastFirst = "", title = "Book"),
          ),
        displayPrefs =
          DisplayPrefs(bookSort = BookSort.AuthorLastFirst, sortOrder = SortOrder.Desc),
      )

    assertEquals(listOf("smith", "hopper", "blank"), sorted.map(BookUiState::id))
  }

  @Test
  fun bookFilterAndSort_whenAuthorAndTitleMatch_preservesCacheOrder() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "first", authorFirstLast = "Author", title = "Title"),
            book(id = "second", authorFirstLast = "Author", title = "Title"),
          ),
        displayPrefs = DisplayPrefs(bookSort = BookSort.AuthorFirstLast),
      )

    assertEquals(listOf("first", "second"), sorted.map(BookUiState::id))
  }

  @Test
  fun bookFilterAndSort_whenDurationAscending_ordersByDuration() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "long", duration = 300.0),
            book(id = "short-first", duration = 100.0),
            book(id = "short-second", duration = 100.0),
            book(id = "medium", duration = 200.0),
          ),
        displayPrefs = DisplayPrefs(bookSort = BookSort.Duration, sortOrder = SortOrder.Asc),
      )

    assertEquals(
      listOf("short-first", "short-second", "medium", "long"),
      sorted.map(BookUiState::id),
    )
  }

  @Test
  fun bookFilterAndSort_whenDurationDescending_ordersByDuration() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "medium", duration = 200.0),
            book(id = "long-first", duration = 300.0),
            book(id = "long-second", duration = 300.0),
            book(id = "short", duration = 100.0),
          ),
        displayPrefs = DisplayPrefs(bookSort = BookSort.Duration, sortOrder = SortOrder.Desc),
      )

    assertEquals(
      listOf("long-first", "long-second", "medium", "short"),
      sorted.map(BookUiState::id),
    )
  }

  @Test
  fun podcastFilterAndSort_whenDownloadedOnly_keepsPodcastsWithDownloadedEpisodes() {
    val filtered =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(title = "Downloaded", downloadedCount = 1),
            podcast(title = "Remote"),
          ),
        displayPrefs = DisplayPrefs(filter = Filter.Downloaded),
      )

    assertEquals(listOf("Downloaded"), filtered.map(PodcastUiState::title))
  }

  @Test
  fun podcastFilterAndSort_whenAuthorAsc_ordersCaseInsensitivelyThenTitleAndMissingLast() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(id = "zulu", author = "Ada Lovelace", title = "Zulu"),
            podcast(id = "alpha", author = "ada lovelace", title = "Alpha"),
            podcast(id = "blank", author = "", title = "Blank"),
            podcast(id = "bravo", author = "Grace Hopper", title = "Bravo"),
          ),
        displayPrefs =
          DisplayPrefs(podcastSort = PodcastSort.Author, podcastSortOrder = SortOrder.Asc),
      )

    assertEquals(listOf("alpha", "zulu", "bravo", "blank"), sorted.map(PodcastUiState::id))
  }

  @Test
  fun podcastFilterAndSort_whenAuthorDesc_keepsMissingAuthorsLast() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(id = "smith", author = "Smith", title = "Podcast"),
            podcast(id = "hopper", author = "Hopper", title = "Podcast"),
            podcast(id = "blank", author = "", title = "Podcast"),
          ),
        displayPrefs =
          DisplayPrefs(podcastSort = PodcastSort.Author, podcastSortOrder = SortOrder.Desc),
      )

    assertEquals(listOf("smith", "hopper", "blank"), sorted.map(PodcastUiState::id))
  }

  @Test
  fun podcastFilterAndSort_whenAuthorsAreBlank_usesTitleAsSecondaryKey() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(id = "spaces", author = " ", title = "Zulu"),
            podcast(id = "empty", author = "", title = "Alpha"),
          ),
        displayPrefs =
          DisplayPrefs(podcastSort = PodcastSort.Author, podcastSortOrder = SortOrder.Asc),
      )

    assertEquals(listOf("empty", "spaces"), sorted.map(PodcastUiState::id))
  }

  @Test
  fun podcastFilterAndSort_whenAuthorAndTitleMatch_preservesCacheOrder() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(id = "first", author = "Author", title = "Title"),
            podcast(id = "second", author = "Author", title = "Title"),
          ),
        displayPrefs = DisplayPrefs(podcastSort = PodcastSort.Author),
      )

    assertEquals(listOf("first", "second"), sorted.map(PodcastUiState::id))
  }

  @Test
  fun podcastFilterAndSort_whenSortByProgressDesc_ordersByRecencyAndUsesTitleTieBreak() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(title = "Zulu", progressLastUpdate = 0L),
            podcast(title = "Charlie", progressLastUpdate = 200L),
            podcast(title = "Alpha", progressLastUpdate = 0L),
            podcast(title = "Bravo", progressLastUpdate = 200L),
          ),
        displayPrefs =
          DisplayPrefs(
            filter = Filter.All,
            podcastSort = PodcastSort.Progress,
            podcastSortOrder = SortOrder.Desc,
          ),
      )

    assertEquals(listOf("Bravo", "Charlie", "Alpha", "Zulu"), sorted.map(PodcastUiState::title))
  }

  @Test
  fun podcastFilterAndSort_whenSortByProgressAsc_ordersByRecencyAndUsesTitleTieBreak() {
    val sorted =
      podcastFilterAndSort(
        podcasts =
          listOf(
            podcast(title = "Zulu", progressLastUpdate = 0L),
            podcast(title = "Charlie", progressLastUpdate = 200L),
            podcast(title = "Alpha", progressLastUpdate = 0L),
            podcast(title = "Bravo", progressLastUpdate = 200L),
          ),
        displayPrefs =
          DisplayPrefs(
            filter = Filter.All,
            podcastSort = PodcastSort.Progress,
            podcastSortOrder = SortOrder.Asc,
          ),
      )

    assertEquals(listOf("Alpha", "Zulu", "Bravo", "Charlie"), sorted.map(PodcastUiState::title))
  }

  private fun podcast(
    title: String,
    id: String = title.lowercase(),
    author: String = "",
    progressLastUpdate: Long = 0L,
    downloadedCount: Int = 0,
  ): PodcastUiState {
    return PodcastUiState(
      id = id,
      author = author,
      title = title,
      progressLastUpdate = progressLastUpdate,
      downloadedCount = downloadedCount,
    )
  }

  private fun book(
    id: String,
    authorFirstLast: String = "",
    authorLastFirst: String = authorFirstLast,
    title: String = id,
    duration: Double = 0.0,
  ): BookUiState {
    return BookUiState(
      id = id,
      author = authorFirstLast,
      authorFirstLast = authorFirstLast,
      authorLastFirst = authorLastFirst,
      title = title,
      duration = duration,
    )
  }
}
