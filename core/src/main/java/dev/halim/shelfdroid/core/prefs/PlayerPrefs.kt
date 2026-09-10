package dev.halim.shelfdroid.core.prefs

import kotlinx.serialization.Serializable

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
