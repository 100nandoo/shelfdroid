package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import okio.Path.Companion.toPath

expect fun createDataStore(): DataStore<Preferences>

fun getDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { producePath().toPath() }
    )

internal const val dataStoreFileName = "shelfdroid.preferences_pb"

class DataStoreKeys {
    val TOKEN = stringPreferencesKey("token")
}