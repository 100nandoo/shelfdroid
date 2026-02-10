package dev.halim.shelfdroid.core.data.prefs

import dev.halim.shelfdroid.core.CrudPrefs
import dev.halim.shelfdroid.core.ListeningSessionPrefs
import dev.halim.shelfdroid.core.PlaybackPrefs
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class PrefsRepository @Inject constructor(private val dataStoreManager: DataStoreManager) {

  val serverPrefs = dataStoreManager.serverPrefs
  val userPrefs = dataStoreManager.userPrefs
  val displayPrefs = dataStoreManager.displayPrefs
  val playbackPrefs = dataStoreManager.playbackPrefs
  val crudPrefs = dataStoreManager.crudPrefs
  val listeningSessionPrefs = dataStoreManager.listeningSessionPrefs

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

  suspend fun updateCrudPrefs(crudPrefs: CrudPrefs) {
    dataStoreManager.updateCrudPrefs(crudPrefs)
  }

  suspend fun updateListeningSessionPrefs(prefs: ListeningSessionPrefs) {
    dataStoreManager.updateListeningSessionPrefs(prefs)
  }

  suspend fun updateHardDelete(enabled: Boolean) {
    val crudPrefs = crudPrefs.first().copy(episodeHardDelete = enabled)
    updateCrudPrefs(crudPrefs)
  }

  suspend fun updateAutoSelectFinished(enabled: Boolean) {
    val crudPrefs = crudPrefs.first().copy(episodeAutoSelectFinished = enabled)
    updateCrudPrefs(crudPrefs)
  }

  suspend fun updateHideDownloaded(enabled: Boolean) {
    val crudPrefs = crudPrefs.first().copy(addEpisodeHideDownloaded = enabled)
    updateCrudPrefs(crudPrefs)
  }
}
