package dev.halim.shelfdroid.datastore

import dev.halim.shelfdroid.ContextUtils

actual fun createDataStoreManager(): DataStoreManager {
    val context = ContextUtils.context

    val dataStoreManager = DataStoreManager {
        context.filesDir.resolve(dataStoreFileName).absolutePath
    }

    return dataStoreManager
}