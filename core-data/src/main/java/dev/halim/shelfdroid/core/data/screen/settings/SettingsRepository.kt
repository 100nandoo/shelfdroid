package dev.halim.shelfdroid.core.data.screen.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  private val prefsRepository: PrefsRepository,
  private val downloadRepo: DownloadRepo,
  private val database: MyDatabase,
  @ApplicationContext private val context: Context,
) {

  val darkMode = dataStoreManager.darkMode
  val dynamicTheme = dataStoreManager.dynamicTheme
  val authPromptReason = dataStoreManager.authPromptReason
  val token = prefsRepository.userPrefs.map { it.accessToken }
  val prefs = prefsRepository.prefsFlow()

  suspend fun fullLogout(): Result<Unit> {
    val refreshToken = prefsRepository.userPrefs.first().refreshToken
    if (refreshToken.isBlank()) {
      return Result.failure(
        IllegalStateException(
          "Unable to log out because the current session is missing its refresh token."
        )
      )
    }

    val remoteLogoutResult = api.logout(refreshToken)
    remoteLogoutResult.exceptionOrNull()?.let {
      return Result.failure(it)
    }

    return clearLocalSessionAndAppData()
  }

  suspend fun logoutForAccountSwitch(): Result<Unit> {
    val refreshToken = prefsRepository.userPrefs.first().refreshToken
    if (refreshToken.isNotBlank()) {
      api.logout(refreshToken)
    }

    return clearLocalSessionAndAppData()
  }

  private suspend fun clearLocalSessionAndAppData(): Result<Unit> {
    return runCatching {
      dataStoreManager.clear()
      downloadRepo.clearTransientDownloads()
      clearLocalDatabase()
      clearAppStorage()
    }
  }

  suspend fun startManualReLogin() {
    dataStoreManager.beginForcedReLogin(AuthPromptReason.ManualReLogin)
  }

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

  private fun clearLocalDatabase() {
    database.libraryEntityQueries.transaction {
      database.localSessionEntityQueries.deleteAll()
      database.progressEntityQueries.deleteAll()
      database.bookmarkEntityQueries.deleteAll()
      database.listeningStatEntityQueries.deleteAll()
      database.podcastEpisodeEntityQueries.deleteAll()
      database.bookEntityQueries.deleteAll()
      database.podcastEntityQueries.deleteAll()
      database.libraryItemEntityQueries.deleteAll()
      database.libraryEntityQueries.deleteAll()
      database.userEntityQueries.deleteAll()
    }
  }

  private fun clearAppStorage() {
    context.cacheDir.deleteRecursively()
    context.externalCacheDir?.deleteRecursively()
  }
}
