package dev.halim.shelfdroid.core.data.screen.settings.player

import dev.halim.shelfdroid.core.prefs.ChapterTimeDisplay
import dev.halim.shelfdroid.core.prefs.DEFAULT_SEEK_INTERVAL_SECONDS

data class SettingsPlayerUiState(
  val chapterTitleLine: Int = 2,
  val chapterTimeDisplay: ChapterTimeDisplay = ChapterTimeDisplay.TimeRange,
  val seekBackSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
  val seekForwardSeconds: Int = DEFAULT_SEEK_INTERVAL_SECONDS,
)
