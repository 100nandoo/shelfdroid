package dev.halim.shelfdroid.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsDefaultsTest {

  @Test
  fun displayPrefs_defaultsPodcastLibraryToProgressDescending() {
    val prefs = DisplayPrefs()

    assertEquals(PodcastSort.Progress, prefs.podcastSort)
    assertEquals(SortOrder.Desc, prefs.podcastSortOrder)
  }

  @Test
  fun podcastSort_exposesAuthorCriterionInHomeOrder() {
    assertEquals(
      listOf(PodcastSort.AddedAt, PodcastSort.Author, PodcastSort.Title, PodcastSort.Progress),
      PodcastSort.entries,
    )
  }

  @Test
  fun podcastSort_fromLabel_readsAuthorCriterion() {
    assertEquals(PodcastSort.Author, PodcastSort.fromLabel("Author"))
  }

  @Test
  fun progressSort_usesLastUpdatedLabelForBothLibraries() {
    assertEquals("Progress: Last Updated", LABEL_PROGRESS)
    assertEquals(BookSort.Progress, BookSort.fromLabel("Progress: Last Updated"))
    assertEquals(PodcastSort.Progress, PodcastSort.fromLabel("Progress: Last Updated"))
  }

  @Test
  fun displayPrefs_selectPodcastSort_resetsOnlyWhenSwitchingToAuthor() {
    val descending =
      DisplayPrefs(podcastSort = PodcastSort.Title, podcastSortOrder = SortOrder.Desc)

    assertEquals(
      DisplayPrefs(podcastSort = PodcastSort.Author, podcastSortOrder = SortOrder.Asc),
      descending.selectPodcastSort(PodcastSort.Author),
    )
    assertEquals(
      DisplayPrefs(podcastSort = PodcastSort.Author, podcastSortOrder = SortOrder.Desc),
      DisplayPrefs(podcastSort = PodcastSort.Author, podcastSortOrder = SortOrder.Desc)
        .selectPodcastSort(PodcastSort.Author),
    )
  }

  @Test
  fun bookSort_exposesAuthorCriteriaInHomeOrder() {
    assertEquals(
      listOf(
        BookSort.AddedAt,
        BookSort.AuthorFirstLast,
        BookSort.AuthorLastFirst,
        BookSort.Duration,
        BookSort.Title,
        BookSort.Progress,
      ),
      BookSort.entries,
    )
  }

  @Test
  fun bookSort_fromLabel_readsBothAuthorCriteria() {
    assertEquals(BookSort.AuthorFirstLast, BookSort.fromLabel("Author (First Last)"))
    assertEquals(BookSort.AuthorLastFirst, BookSort.fromLabel("Author (Last, First)"))
  }

  @Test
  fun displayPrefs_selectBookSort_resetsOnlyWhenSwitchingToAnAuthorCriterion() {
    val descending = DisplayPrefs(bookSort = BookSort.Title, sortOrder = SortOrder.Desc)

    assertEquals(
      DisplayPrefs(bookSort = BookSort.AuthorFirstLast, sortOrder = SortOrder.Asc),
      descending.selectBookSort(BookSort.AuthorFirstLast),
    )
    assertEquals(
      DisplayPrefs(bookSort = BookSort.AuthorFirstLast, sortOrder = SortOrder.Desc),
      DisplayPrefs(bookSort = BookSort.AuthorFirstLast, sortOrder = SortOrder.Desc)
        .selectBookSort(BookSort.AuthorFirstLast),
    )
  }

  @Test
  fun playerPrefs_defaultsChapterTimeDisplayToShortDuration() {
    val prefs = PlayerPrefs()

    assertEquals(ChapterTimeDisplay.DurationShort, prefs.chapterTimeDisplay)
  }
}
