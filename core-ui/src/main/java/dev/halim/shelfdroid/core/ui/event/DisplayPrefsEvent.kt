package dev.halim.shelfdroid.core.ui.event

sealed interface DisplayPrefsEvent {
  data class Filter(val filter: String) : DisplayPrefsEvent

  data class BookSort(val bookSort: String) : DisplayPrefsEvent

  data class PodcastSort(val podcastSort: String) : DisplayPrefsEvent

  data class SortOrder(val sortOrder: String) : DisplayPrefsEvent

  data class PodcastSortOrder(val sortOrder: String) : DisplayPrefsEvent
}
