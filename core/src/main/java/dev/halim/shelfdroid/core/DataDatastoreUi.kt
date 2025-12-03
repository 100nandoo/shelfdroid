package dev.halim.shelfdroid.core

import kotlinx.serialization.Serializable

@Serializable
data class UserPrefs(
  val id: String = "",
  val username: String = "",
  val isAdmin: Boolean = false,
  val download: Boolean = false,
  val update: Boolean = false,
  val delete: Boolean = false,
  val upload: Boolean = false,
  val accessToken: String = "",
  val refreshToken: String = "",
)

@Serializable data class ServerPrefs(val version: String = "")

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

const val LABEL_ADDED_AT = "Added At"
const val LABEL_DURATION = "Duration"
const val LABEL_TITLE = "Title"
const val LABEL_PROGRESS = "Progress"

enum class BookSort(val label: String) {
  AddedAt(LABEL_ADDED_AT),
  Duration(LABEL_DURATION),
  Title(LABEL_TITLE),
  Progress(LABEL_PROGRESS);

  companion object {
    fun fromLabel(label: String): BookSort {
      return when (label) {
        LABEL_ADDED_AT -> AddedAt
        LABEL_DURATION -> Duration
        LABEL_TITLE -> Title
        LABEL_PROGRESS -> Progress
        else -> AddedAt
      }
    }
  }
}

enum class PodcastSort(val label: String) {
  AddedAt(LABEL_ADDED_AT),
  Title(LABEL_TITLE);

  companion object {
    fun fromLabel(label: String): PodcastSort {
      return when (label) {
        LABEL_ADDED_AT -> AddedAt
        LABEL_TITLE -> Title
        else -> AddedAt
      }
    }
  }
}

enum class SortOrder {
  Asc,
  Desc,
}

@Serializable
data class DisplayPrefs(
  val listView: Boolean = true,
  val filter: Filter = Filter.All,
  val bookSort: BookSort = BookSort.AddedAt,
  val podcastSort: PodcastSort = PodcastSort.AddedAt,
  val sortOrder: SortOrder = SortOrder.Asc,
  val podcastSortOrder: SortOrder = SortOrder.Asc,
)

@Serializable
data class Prefs(
  val userPrefs: UserPrefs = UserPrefs(),
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
)
