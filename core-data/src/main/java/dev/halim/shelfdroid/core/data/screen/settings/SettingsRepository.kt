package dev.halim.shelfdroid.core.data.screen.settings

import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SettingsRepository
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
  private val prefsRepository: PrefsRepository,
) {

  val darkMode = dataStoreManager.darkMode
  val dynamicTheme = dataStoreManager.dynamicTheme
  val prefs = prefsRepository.prefsFlow()

  suspend fun updateDarkMode(enabled: Boolean) {
    dataStoreManager.updateDarkMode(enabled)
  }

  suspend fun updateDynamicTheme(enabled: Boolean) {
    dataStoreManager.updateDynamicTheme(enabled)
  }

  suspend fun updateListView(enabled: Boolean) {
    dataStoreManager.updateListView(enabled)
  }

  suspend fun updateHardDelete(enabled: Boolean) {
    val crudPrefs = prefsRepository.crudPrefs.first().copy(hardDelete = enabled)
    prefsRepository.updateCrudPrefs(crudPrefs)
  }

  suspend fun updateFilter(filter: Filter) {
    dataStoreManager.updateFilter(filter)
  }

  suspend fun updateBookSort(bookSort: BookSort) {
    dataStoreManager.updateBookSort(bookSort)
  }

  suspend fun updatePodcastSort(podcastSort: PodcastSort) {
    dataStoreManager.updatePodcastSort(podcastSort)
  }

  suspend fun updateSortOrder(sortOrder: SortOrder) {
    dataStoreManager.updateSortOrder(sortOrder)
  }

  suspend fun updatePodcastSortOrder(podcastSortOrder: SortOrder) {
    dataStoreManager.updatePodcastSortOrder(podcastSortOrder)
  }

}
