package dev.halim.shelfdroid.core.data.screen.settingsplayback

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsPlaybackRepository
@Inject
constructor(private val dataStoreManager: DataStoreManager) {
  val playbackPrefs = dataStoreManager.playbackPrefs

  suspend fun updateKeepSpeed(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(keepSpeed = enabled)
    dataStoreManager.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateKeepSleepTimer(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(keepSleepTimer = enabled)
    dataStoreManager.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateEpisodeKeepSpeed(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(episodeKeepSpeed = enabled)
    dataStoreManager.updatePlaybackPrefs(playbackPrefs)
  }

  suspend fun updateEpisodeKeepSleepTimer(enabled: Boolean) {
    val playbackPrefs = playbackPrefs.first().copy(episodeKeepSleepTimer = enabled)
    dataStoreManager.updatePlaybackPrefs(playbackPrefs)
  }
}
