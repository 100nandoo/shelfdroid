package dev.halim.shelfdroid.core.data.screen.settingsplayback

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsPlaybackRepository @Inject constructor(private val prefsRepository: PrefsRepository) {
  val playbackPrefs = prefsRepository.playbackPrefs

  suspend fun updateKeepSpeed(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(keepSpeed = enabled)
    prefsRepository.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateKeepSleepTimer(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(keepSleepTimer = enabled)
    prefsRepository.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateEpisodeKeepSpeed(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(episodeKeepSpeed = enabled)
    prefsRepository.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateEpisodeKeepSleepTimer(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(episodeKeepSleepTimer = enabled)
    prefsRepository.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateBookKeepSpeed(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(bookKeepSpeed = enabled)
    prefsRepository.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateBookKeepSleepTimer(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(bookKeepSleepTimer = enabled)
    prefsRepository.updatePlaybackPrefs(playbackPrefs)
  }
}
