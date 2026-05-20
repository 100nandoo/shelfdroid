package dev.halim.shelfdroid.core.data.screen.settings.player

import dev.halim.shelfdroid.core.ChapterTimeDisplay
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsPlayerRepository @Inject constructor(private val prefsRepository: PrefsRepository) {
  val playerPrefs = prefsRepository.playerPrefs

  suspend fun updateChapterTitleLine(chapterTitleLine: Int) {
    val current = playerPrefs.first().copy(chapterTitleLine = chapterTitleLine)
    prefsRepository.updatePlayerPrefs(current)
  }

  suspend fun updateChapterTimeDisplay(chapterTimeDisplay: ChapterTimeDisplay) {
    val current = playerPrefs.first().copy(chapterTimeDisplay = chapterTimeDisplay)
    prefsRepository.updatePlayerPrefs(current)
  }
}
