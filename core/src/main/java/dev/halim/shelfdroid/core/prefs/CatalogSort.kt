package dev.halim.shelfdroid.core.prefs

const val LABEL_ADDED_AT = "Added At"
const val LABEL_AUTHOR = "Author"
const val LABEL_AUTHOR_FIRST_LAST = "Author (First Last)"
const val LABEL_AUTHOR_LAST_FIRST = "Author (Last, First)"
const val LABEL_DURATION = "Duration"
const val LABEL_TITLE = "Title"
const val LABEL_PROGRESS = "Progress: Last Updated"

private const val LEGACY_LABEL_PROGRESS = "Progress"

enum class BookSort(val label: String) {
  AddedAt(LABEL_ADDED_AT),
  AuthorFirstLast(LABEL_AUTHOR_FIRST_LAST),
  AuthorLastFirst(LABEL_AUTHOR_LAST_FIRST),
  Duration(LABEL_DURATION),
  Title(LABEL_TITLE),
  Progress(LABEL_PROGRESS);

  val isAuthorSort: Boolean
    get() = this == AuthorFirstLast || this == AuthorLastFirst

  companion object {
    fun fromLabel(label: String): BookSort {
      return when (label) {
        LABEL_ADDED_AT -> AddedAt
        LABEL_AUTHOR_FIRST_LAST -> AuthorFirstLast
        LABEL_AUTHOR_LAST_FIRST -> AuthorLastFirst
        LABEL_DURATION -> Duration
        LABEL_TITLE -> Title
        LABEL_PROGRESS,
        LEGACY_LABEL_PROGRESS -> Progress
        else -> AddedAt
      }
    }
  }
}

enum class PodcastSort(val label: String) {
  AddedAt(LABEL_ADDED_AT),
  Author(LABEL_AUTHOR),
  Title(LABEL_TITLE),
  Progress(LABEL_PROGRESS);

  companion object {
    fun fromLabel(label: String): PodcastSort {
      return when (label) {
        LABEL_ADDED_AT -> AddedAt
        LABEL_AUTHOR -> Author
        LABEL_TITLE -> Title
        LABEL_PROGRESS,
        LEGACY_LABEL_PROGRESS -> Progress
        else -> AddedAt
      }
    }
  }
}
