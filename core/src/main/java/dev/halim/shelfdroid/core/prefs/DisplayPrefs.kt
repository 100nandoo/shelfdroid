package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

enum class SortOrder {
  Asc,
  Desc,
}

@Serializable
data class DisplayPrefs(
  val listView: Boolean = true,
  val filter: Filter = Filter.All,
  val bookSort: BookSort = BookSort.Progress,
  val podcastSort: PodcastSort = PodcastSort.Progress,
  val sortOrder: SortOrder = SortOrder.Desc,
  val podcastSortOrder: SortOrder = SortOrder.Desc,
) {
  fun selectBookSort(bookSort: BookSort): DisplayPrefs {
    val resetOrder = bookSort.isAuthorSort && bookSort != this.bookSort
    return copy(
      bookSort = bookSort,
      sortOrder = if (resetOrder) SortOrder.Asc else sortOrder,
    )
  }

  fun selectPodcastSort(podcastSort: PodcastSort): DisplayPrefs {
    val resetOrder = podcastSort == PodcastSort.Author && podcastSort != this.podcastSort
    return copy(
      podcastSort = podcastSort,
      podcastSortOrder = if (resetOrder) SortOrder.Asc else podcastSortOrder,
    )
  }
}

enum class Filter {
  All,
  Downloaded;

  fun toggleDownloaded(): Filter =
    when (this) {
      All -> Downloaded
      Downloaded -> All
    }

  fun isDownloaded(): Boolean = this == Downloaded
}
