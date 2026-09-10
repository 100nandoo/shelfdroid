package dev.halim.shelfdroid.core.data.screen.settings.player

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.prefs.ChapterTimeDisplay
import dev.halim.shelfdroid.core.prefs.PlayerPrefs
import javax.inject.Inject

class SettingsPlayerRepository @Inject constructor(private val prefsRepository: PrefsRepository) {
  val playerPrefs = prefsRepository.playerPrefs

  suspend fun updateChapterTitleLine(chapterTitleLine: Int) {
    updatePlayerPrefs { it.copy(chapterTitleLine = chapterTitleLine) }
  }

  suspend fun updateChapterTimeDisplay(chapterTimeDisplay: ChapterTimeDisplay) {
    updatePlayerPrefs { it.copy(chapterTimeDisplay = chapterTimeDisplay) }
  }

  suspend fun updateSeekBackSeconds(seconds: Int) {
    updatePlayerPrefs { it.copy(seekBackSeconds = seconds) }
  }

  suspend fun updateSeekForwardSeconds(seconds: Int) {
    updatePlayerPrefs { it.copy(seekForwardSeconds = seconds) }
  }

  private suspend fun updatePlayerPrefs(update: (PlayerPrefs) -> PlayerPrefs) {
    prefsRepository.updatePlayerPrefs(update)
  }
}
