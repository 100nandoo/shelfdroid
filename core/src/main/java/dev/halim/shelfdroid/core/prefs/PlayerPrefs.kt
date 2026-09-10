package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

val CHAPTER_TITLE_PRESET_LINE: List<Int> = listOf(1, 2, 3)
const val DEFAULT_SEEK_INTERVAL_SECONDS = 10
val SEEK_INTERVAL_OPTIONS: List<Int> = listOf(DEFAULT_SEEK_INTERVAL_SECONDS, 15, 30, 60)

enum class ChapterTimeDisplay {
  TimeRange,
  Duration,
  DurationShort,
}

@Serializable
data class PlayerPrefs(
  val chapterTitleLine: Int = 2,
  val chapterTimeDisplay: ChapterTimeDisplay = ChapterTimeDisplay.DurationShort,
  val seekBackSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
  val seekForwardSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
)
