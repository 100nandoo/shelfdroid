package dev.halim.shelfdroid.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private object Keys {
    val BASE_URL = stringPreferencesKey("base_url")
    val TOKEN = stringPreferencesKey("token")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
}

private fun <T> DataStore<Preferences>.preferenceFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
    data.map { preferences -> preferences[key] ?: defaultValue }

private suspend fun <T> DataStore<Preferences>.updatePreference(key: Preferences.Key<T>, value: T) {
    edit { preferences -> preferences[key] = value }
}

class DataStoreManager(private val dataStore: DataStore<Preferences>) {
    val baseUrl: Flow<String> = dataStore.preferenceFlow(Keys.BASE_URL, "dummy base url")
    suspend fun updateBaseUrl(baseUrl: String) = dataStore.updatePreference(Keys.BASE_URL, baseUrl)

    val token: Flow<String> = dataStore.preferenceFlow(Keys.TOKEN, "")
    suspend fun updateToken(token: String) = dataStore.updatePreference(Keys.TOKEN, token)

    val darkMode: Flow<Boolean> = dataStore.preferenceFlow(Keys.DARK_MODE, true)
    suspend fun updateDarkMode(darkMode: Boolean) = dataStore.updatePreference(Keys.DARK_MODE, darkMode)
}