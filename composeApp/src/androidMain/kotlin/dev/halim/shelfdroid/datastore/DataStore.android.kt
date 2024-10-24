package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.halim.shelfdroid.ContextUtils

actual fun createDataStore(): DataStore<Preferences> = getDataStore(
    producePath = { ContextUtils.context.filesDir.resolve(dataStoreFileName).absolutePath }
)