package dev.halim.shelfdroid.core.data.screen.settings.player

import dev.halim.shelfdroid.core.ChapterTimeDisplay

data class SettingsPlayerUiState(
  val chapterTitleLine: Int = 2,
  val chapterTimeDisplay: ChapterTimeDisplay = ChapterTimeDisplay.TimeRange,
)
