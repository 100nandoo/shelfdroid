package dev.halim.shelfdroid.core.data

import dev.halim.shelfdroid.core.datastore.DataStoreManager

data class UserPrefs(
    private val dataStoreManager: DataStoreManager,
    var token: String,
    var baseUrl: String,
    val deviceId: String,
    val darkMode: Boolean
) {

    suspend fun changeServer(baseUrl: String, token: String) {
        this.baseUrl = baseUrl
        this.token = token
        dataStoreManager.updateBaseUrl(baseUrl)
        dataStoreManager.updateToken(token)
    }
}
