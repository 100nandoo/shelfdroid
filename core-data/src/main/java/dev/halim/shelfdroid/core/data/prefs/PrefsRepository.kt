package dev.halim.shelfdroid.core.data.prefs

import dev.halim.shelfdroid.core.PlaybackPrefs
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PrefsRepository @Inject constructor(private val dataStoreManager: DataStoreManager) {

  val serverPrefs = dataStoreManager.serverPrefs
  val userPrefs = dataStoreManager.userPrefs
  val displayPrefs = dataStoreManager.displayPrefs
  val playbackPrefs = dataStoreManager.playbackPrefs
  val crudPrefs = dataStoreManager.crudPrefs

  fun prefsFlow(): Flow<Prefs> {
    return combine(userPrefs, displayPrefs, crudPrefs) { userPrefs, displayPrefs, crudPrefs ->
      Prefs(userPrefs, displayPrefs, crudPrefs)
    }
  }

  suspend fun updateUserPrefs(userPrefs: UserPrefs) {
    dataStoreManager.updateUserPrefs(userPrefs)
  }

  suspend fun updateServerPrefs(serverPrefs: ServerPrefs) {
    dataStoreManager.updateServerPrefs(serverPrefs)
  }

  suspend fun updatePlaybackPrefs(playbackPrefs: PlaybackPrefs) {
    dataStoreManager.updatePlaybackPrefs(playbackPrefs)
  }
}
