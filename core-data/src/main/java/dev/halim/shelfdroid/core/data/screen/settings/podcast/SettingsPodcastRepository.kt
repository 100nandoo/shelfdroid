package dev.halim.shelfdroid.core.data.screen.settings.podcast

import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsPodcastRepository
@Inject
constructor(
  private val prefsRepository: PrefsRepository,
  private val dataStoreManager: DataStoreManager,
) {

  val prefs = prefsRepository.prefsFlow()

  suspend fun updateHardDelete(enabled: Boolean) {
    val crudPrefs = prefsRepository.crudPrefs.first().copy(episodeHardDelete = enabled)
    dataStoreManager.updateCrudPrefs(crudPrefs)
  }
}
