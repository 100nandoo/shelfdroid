package dev.halim.shelfdroid.core.ui.screen.home

import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
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
  fun bookFilterAndSort_whenProgressAscending_keepsNoProgressLast() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "no-progress", progressLastUpdate = 0L),
            book(id = "older", progressLastUpdate = 100L),
            book(id = "newer", progressLastUpdate = 200L),
          ),
        displayPrefs = DisplayPrefs(bookSort = BookSort.Progress, sortOrder = SortOrder.Asc),
      )

    assertEquals(listOf("older", "newer", "no-progress"), sorted.map(BookUiState::id))
  }

  @Test
  fun bookFilterAndSort_whenProgressDescending_keepsNoProgressLast() {
    val sorted =
      bookFilterAndSort(
        books =
          listOf(
            book(id = "no-progress", progressLastUpdate = 0L),
            book(id = "older", progressLastUpdate = 100L),
            book(id = "newer", progressLastUpdate = 200L),
          ),
        displayPrefs = DisplayPrefs(bookSort = BookSort.Progress, sortOrder = SortOrder.Desc),
      )

    assertEquals(listOf("newer", "older", "no-progress"), sorted.map(BookUiState::id))
  }

  @Test
  fun bookSecondaryText_whenDurationSort_formatsTotalRuntime() {
    val book = book(id = "book", duration = 3660.0)

    assertEquals("1h 1m", book.secondaryText(BookSort.Duration, "Unknown"))
  }

  @Test
  fun bookSecondaryText_whenDurationIsMissing_usesUnknownText() {
    assertEquals(
      "Unknown",
      book(id = "book", duration = 0.0).secondaryText(BookSort.Duration, "Unknown"),
    )
    assertEquals(
      "Unknown",
      book(id = "book", duration = -1.0).secondaryText(BookSort.Duration, "Unknown"),
    )
  }

  @Test
  fun formatAddedDate_formatsSingleDigitDayWithAbbreviatedMonth() {
    val addedAt =
      ZonedDateTime.of(2026, 1, 7, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    assertEquals(
      "7 Jan 2026",
      formatAddedDate(addedAt, locale = Locale.ENGLISH, zoneId = ZoneId.of("UTC")),
    )
  }

  @Test
  fun formatAddedDate_formatsDoubleDigitDayWithAbbreviatedMonth() {
    val addedAt =
      ZonedDateTime.of(2025, 12, 27, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    assertEquals(
      "27 Dec 2025",
      formatAddedDate(addedAt, locale = Locale.ENGLISH, zoneId = ZoneId.of("UTC")),
    )
  }

  @Test
  fun formatAddedDate_usesLocalTimeZoneForCalendarDate() {
    val addedAt = Instant.parse("2026-01-07T23:30:00Z").toEpochMilli()

    assertEquals(
      "8 Jan 2026",
      formatAddedDate(addedAt, locale = Locale.ENGLISH, zoneId = ZoneId.of("Asia/Singapore")),
    )
  }

  @Test
  fun formatAddedDateSecondaryText_whenTimestampIsMissing_usesUnknownText() {
    assertEquals(
      "Unknown",
      formatAddedDateSecondaryText(
        addedAt = 0,
        addedDateLabel = "Added %1\$s",
        unknownAddedDate = "Unknown",
        locale = Locale.ENGLISH,
        zoneId = ZoneId.of("UTC"),
      ),
    )
  }

  @Test
  fun formatAddedDateSecondaryText_whenTimestampIsPresent_appliesAddedLabel() {
    val addedAt =
      ZonedDateTime.of(2025, 12, 27, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    assertEquals(
      "Added 27 Dec 2025",
      formatAddedDateSecondaryText(
        addedAt = addedAt,
        addedDateLabel = "Added %1\$s",
        unknownAddedDate = "Unknown",
        locale = Locale.ENGLISH,
        zoneId = ZoneId.of("UTC"),
      ),
    )
  }

  @Test
  fun bookSecondaryText_whenAddedAt_usesAddedDateText() {
    val book =
      book(
        id = "book",
        addedAt =
          ZonedDateTime.of(2026, 1, 7, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli(),
      )

    assertEquals(
      "Added 7 Jan 2026",
      book.secondaryText(
        BookSort.AddedAt,
        "Unknown",
        formatAddedDateSecondaryText(
          addedAt = book.addedAt,
          addedDateLabel = "Added %1\$s",
          unknownAddedDate = "Unknown",
          locale = Locale.ENGLISH,
          zoneId = ZoneId.of("UTC"),
        ),
      ),
    )
  }

  @Test
  fun formatProgressLastUpdated_includesDateYearHourAndMinuteWithoutSeconds() {
    val lastUpdate =
      ZonedDateTime.of(2026, 1, 7, 12, 34, 56, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

    assertEquals(
      "Jan 7, 2026, 12:34\u202fPM",
      formatProgressLastUpdated(lastUpdate, locale = Locale.US, zoneId = ZoneId.of("UTC")),
    )
  }

  @Test
  fun formatProgressLastUpdated_usesDeviceTimeZoneForDisplayedDateAndTime() {
    val lastUpdate = Instant.parse("2026-01-07T23:30:00Z").toEpochMilli()

    assertEquals(
      "Jan 8, 2026, 7:30\u202fAM",
      formatProgressLastUpdated(
        lastUpdate,
        locale = Locale.US,
        zoneId = ZoneId.of("Asia/Singapore"),
      ),
    )
  }

  @Test
  fun formatProgressLastUpdatedSecondaryText_whenTimestampIsMissing_usesFallback() {
    assertEquals(
      "No progress yet",
      formatProgressLastUpdatedSecondaryText(
        lastUpdate = 0L,
        noProgressYet = "No progress yet",
        locale = Locale.US,
        zoneId = ZoneId.of("UTC"),
      ),
    )
  }

  @Test
  fun bookSecondaryText_whenProgressSort_usesLastUpdatedText() {
    assertEquals(
      "Jan 7, 2026, 12:34\u202fPM",
      book(id = "book")
        .secondaryText(
          bookSort = BookSort.Progress,
          unknownDuration = "Unknown",
          progressLastUpdatedText = "Jan 7, 2026, 12:34\u202fPM",
        ),
    )
  }

  @Test
  fun bookSecondaryText_whenNotSortedByDuration_usesAuthorName() {
    val book =
      book(
        id = "book",
        authorFirstLast = "John Doe",
        authorLastFirst = "Doe, John",
        duration = 3660.0,
      )

    assertEquals("John Doe", book.secondaryText(BookSort.AuthorFirstLast, "Unknown"))
    assertEquals("Doe, John", book.secondaryText(BookSort.AuthorLastFirst, "Unknown"))
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
  fun podcastSecondaryText_whenAddedAt_usesAddedDateText() {
    val podcast =
      podcast(
        title = "Podcast",
        addedAt =
          ZonedDateTime.of(2025, 12, 27, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli(),
      )

    assertEquals(
      "Added 27 Dec 2025",
      podcast.secondaryText(
        PodcastSort.AddedAt,
        formatAddedDateSecondaryText(
          addedAt = podcast.addedAt,
          addedDateLabel = "Added %1\$s",
          unknownAddedDate = "Unknown",
          locale = Locale.ENGLISH,
          zoneId = ZoneId.of("UTC"),
        ),
      ),
    )
  }

  @Test
  fun podcastSecondaryText_whenProgressSort_usesLastUpdatedText() {
    assertEquals(
      "Jan 7, 2026, 12:34\u202fPM",
      podcast(title = "Podcast")
        .secondaryText(
          podcastSort = PodcastSort.Progress,
          progressLastUpdatedText = "Jan 7, 2026, 12:34\u202fPM",
        ),
    )
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
            podcast(title = "Echo", progressLastUpdate = 100L),
          ),
        displayPrefs =
          DisplayPrefs(
            filter = Filter.All,
            podcastSort = PodcastSort.Progress,
            podcastSortOrder = SortOrder.Desc,
          ),
      )

    assertEquals(
      listOf("Bravo", "Charlie", "Echo", "Alpha", "Zulu"),
      sorted.map(PodcastUiState::title),
    )
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
            podcast(title = "Echo", progressLastUpdate = 100L),
          ),
        displayPrefs =
          DisplayPrefs(
            filter = Filter.All,
            podcastSort = PodcastSort.Progress,
            podcastSortOrder = SortOrder.Asc,
          ),
      )

    assertEquals(
      listOf("Echo", "Bravo", "Charlie", "Alpha", "Zulu"),
      sorted.map(PodcastUiState::title),
    )
  }

  private fun podcast(
    title: String,
    id: String = title.lowercase(),
    author: String = "",
    addedAt: Long = 0L,
    progressLastUpdate: Long = 0L,
    downloadedCount: Int = 0,
  ): PodcastUiState {
    return PodcastUiState(
      id = id,
      author = author,
      title = title,
      addedAt = addedAt,
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
    addedAt: Long = 0L,
    progressLastUpdate: Long = 0L,
  ): BookUiState {
    return BookUiState(
      id = id,
      author = authorFirstLast,
      authorFirstLast = authorFirstLast,
      authorLastFirst = authorLastFirst,
      title = title,
      duration = duration,
      addedAt = addedAt,
      progressLastUpdate = progressLastUpdate,
    )
  }
}
