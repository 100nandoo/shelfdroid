package dev.halim.shelfdroid.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.halim.shelfdroid.ContextUtils
import kotlinx.coroutines.CoroutineScope
import okio.Path.Companion.toPath

actual fun createDataStore(coroutineScope: CoroutineScope): DataStore<Preferences> {
    val context = ContextUtils.context
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { context.filesDir.resolve(dataStoreFileName).absolutePath.toPath() },
        scope = coroutineScope
    )
}