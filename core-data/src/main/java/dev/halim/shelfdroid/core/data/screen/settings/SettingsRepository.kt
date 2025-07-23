package dev.halim.shelfdroid.core.data.screen.settings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class SettingsRepository
@Inject
constructor(private val api: ApiService, private val dataStoreManager: DataStoreManager) {

  val darkMode = dataStoreManager.darkMode
  val dynamicTheme = dataStoreManager.dynamicTheme
  val token = dataStoreManager.userPrefs.map { it.accessToken }

  suspend fun logout(): Result<Unit> {
    val result = api.logout()
    result.onSuccess { _ ->
      dataStoreManager.clear()
      return Result.success(Unit)
    }
    result.onFailure { error ->
      return Result.failure(error)
    }
    return Result.failure(Exception("Logout failed"))
  }

  suspend fun updateDarkMode(enabled: Boolean) {
    dataStoreManager.updateDarkMode(enabled)
  }

  suspend fun updateDynamicTheme(enabled: Boolean) {
    dataStoreManager.updateDynamicTheme(enabled)
  }
}
