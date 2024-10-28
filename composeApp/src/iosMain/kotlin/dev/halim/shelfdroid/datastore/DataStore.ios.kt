package dev.halim.shelfdroid.datastore

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStoreManager(): DataStoreManager {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )

    requireNotNull(documentDirectory) { "Document directory not found." }

    val dataStoreManager = DataStoreManager {
        documentDirectory.path + "/$dataStoreFileName"
    }

    return dataStoreManager
}