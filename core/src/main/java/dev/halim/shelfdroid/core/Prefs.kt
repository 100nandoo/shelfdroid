package dev.halim.shelfdroid.core

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

@Serializable
data class CrudPrefs(
  // Library item
  val hardDelete: Boolean = true,
  // Episode
  // * delete
  val episodeHardDelete: Boolean = true,
  val episodeAutoSelectFinished: Boolean = true,
  // * add
  val addEpisodeHideDownloaded: Boolean = true,
)

@Serializable
data class Prefs(
  val userPrefs: UserPrefs = UserPrefs(),
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
  val crudPrefs: CrudPrefs = CrudPrefs(),
)

@Serializable
data class PlaybackPrefs(
  val keepSpeed: Boolean = false,
  val keepSleepTimer: Boolean = false,
  val episodeKeepSpeed: Boolean = true,
  val episodeKeepSleepTimer: Boolean = true,
  val bookKeepSpeed: Boolean = true,
  val bookKeepSleepTimer: Boolean = true,
)

@Serializable
enum class MediaNotificationAction {
  SleepTimer,
  NextChapter,
  PlaybackSpeed,
  None,
}

val PLAYBACK_SPEED_PRESET_VALUES: List<Float> =
  listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

val DEFAULT_PLAYBACK_SPEED_CYCLE: List<Float> = listOf(1f, 1.25f, 1.5f, 2f)

@Serializable
data class NotificationPrefs(
  val sleepTimerMinutes: Int = 1,
  val firstAction: MediaNotificationAction = MediaNotificationAction.SleepTimer,
  val secondAction: MediaNotificationAction = MediaNotificationAction.NextChapter,
  val playbackSpeedCycle: List<Float> = DEFAULT_PLAYBACK_SPEED_CYCLE,
)

fun normalizePlaybackSpeedCycle(speeds: List<Float>): List<Float> {
  val normalized =
    speeds
      .filter { it in PLAYBACK_SPEED_PRESET_VALUES }
      .distinct()
      .sorted()
  return if (normalized.size >= 2) normalized else DEFAULT_PLAYBACK_SPEED_CYCLE
}

fun nextPlaybackSpeed(currentSpeed: Float, selectedSpeeds: List<Float>): Float {
  val speeds = normalizePlaybackSpeedCycle(selectedSpeeds)
  return speeds.firstOrNull { it > currentSpeed + 0.001f } ?: speeds.first()
}

val CHAPTER_TITLE_PRESET_LINE: List<Int> = listOf(1, 2, 3)

enum class ChapterTimeDisplay {
  TimeRange,
  Duration,
  DurationShort,
}

@Serializable
data class PlayerPrefs(
  val chapterTitleLine: Int = 2,
  val chapterTimeDisplay: ChapterTimeDisplay = ChapterTimeDisplay.DurationShort,
)

val SLEEP_TIMER_PRESET_MINUTES: List<Int> = listOf(1, 5, 10, 15, 30, 45, 60)

@Serializable
data class ListeningSessionPrefs(val itemsPerPage: Int = 10, val defaultUserId: String? = null)

@Serializable
data class UserPrefs(
  val id: String = "",
  val username: String = "",
  val type: UserType = UserType.Unknown,
  val isAdmin: Boolean = false,
  val download: Boolean = false,
  val update: Boolean = false,
  val delete: Boolean = false,
  val upload: Boolean = false,
  val accessToken: String = "",
  val refreshToken: String = "",
)

@Serializable
enum class ServerAccessMode {
  Internet,
  LocalNetwork,
}

@Serializable
data class ServerPrefs(
  val version: String = "",
  val logLevel: Int = 1,
  val accessMode: ServerAccessMode = ServerAccessMode.Internet,
)

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

enum class ItemsPerPage(val label: Int) {
  I10(10),
  I25(25),
  I50(50),
  I100(100);

  companion object {
    fun fromLabel(label: Int): ItemsPerPage {
      return when (label) {
        10 -> I10
        25 -> I25
        50 -> I50
        100 -> I100
        else -> I10
      }
    }
  }
}
