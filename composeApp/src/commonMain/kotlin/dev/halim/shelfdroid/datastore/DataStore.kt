package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import okio.Path.Companion.toPath

expect fun createDataStoreManager(): DataStoreManager

internal const val dataStoreFileName = "shelfdroid.preferences_pb"

class DataStoreManager(private val producePath: () -> String) {
    val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { producePath().toPath() }
        )
    }

    object DataStoreKeys {
        val TOKEN = stringPreferencesKey("token")
        val BASE_URL = stringPreferencesKey("base_url")
    }
}