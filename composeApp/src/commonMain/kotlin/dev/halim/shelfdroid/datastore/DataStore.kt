package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

expect fun createDataStore(coroutineScope: CoroutineScope): DataStore<Preferences>

internal const val dataStoreFileName = "shelfdroid.preferences_pb"

class DataStoreManager(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TOKEN = stringPreferencesKey("token")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    private fun readString(key: Preferences.Key<String>, default: String = ""): Flow<String> =
        dataStore.data.map { it[key] ?: default }

    private fun readBoolean(
        key: Preferences.Key<Boolean>,
        default: Boolean = false
    ): Flow<Boolean> =
        dataStore.data.map { it[key] ?: default }

    private suspend fun writeString(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun writeBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    suspend fun clear() = dataStore.edit { it.clear() }

    suspend fun setToken(value: String) = writeString(Keys.TOKEN, value)

    val baseUrl: Flow<String> = readString(Keys.BASE_URL)
    suspend fun setBaseUrl(value: String) = writeString(Keys.BASE_URL, value)

    val isDarkMode: Flow<Boolean> = readBoolean(Keys.IS_DARK_MODE)
    suspend fun setDarkMode(value: Boolean) = writeBoolean(Keys.IS_DARK_MODE, value)

    val token: Flow<String> = readString(Keys.TOKEN)
    val tokenBlocking: String?
        get() = runBlocking {
            dataStore.data.firstOrNull()?.get(Keys.TOKEN)
        }
}