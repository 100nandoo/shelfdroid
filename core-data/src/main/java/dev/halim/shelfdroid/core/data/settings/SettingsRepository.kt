package dev.halim.shelfdroid.core.data.settings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager
) {

    val darkMode = dataStoreManager.darkMode
    fun darkMode(): Boolean {
        return runBlocking { darkMode.first() }
    }

    fun isLoggedIn(): Boolean {
        return runBlocking { dataStoreManager.token.first().isBlank().not() }
    }

    suspend fun logout(): Result<Unit> {
        val result = api.logout()
        result.onSuccess { _ ->
//            dataStoreManager.clear()
            return Result.success(Unit)
        }
        result.onFailure { error -> return Result.failure(error) }
        return Result.failure(Exception("Logout failed"))
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStoreManager.updateDarkMode(enabled)
    }
}
