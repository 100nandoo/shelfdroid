package dev.halim.shelfdroid.core.data.settings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager
) {

    val darkMode = dataStoreManager.darkMode
    val token = dataStoreManager.token

    suspend fun logout(): Result<Unit> {
        val result = api.logout()
        result.onSuccess { _ ->
            dataStoreManager.clear()
            return Result.success(Unit)
        }
        result.onFailure { error -> return Result.failure(error) }
        return Result.failure(Exception("Logout failed"))
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStoreManager.updateDarkMode(enabled)
    }
}
