package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.network.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi

sealed class StoreOutput<T> {
    data class Single<T>(val data: T) : StoreOutput<T>()
    data class Collection<T>(val data: List<T>) : StoreOutput<T>()
}

inline fun <reified T> StoreOutput<T>.asCollection(): StoreOutput.Collection<T> {
    return this as StoreOutput.Collection<T>
}

inline fun <reified T> StoreOutput<T>.asSingle(): StoreOutput.Single<T> {
    return this as StoreOutput.Single<T>
}

class StoreManager(api: Api, database: Database, private val io: CoroutineScope) {
    val libraryStore: LibraryStore = LibraryStoreFactory(api, database).create()
    val itemStore: ItemStore = ItemStoreFactory(api, database).create()
    val progressStore: ProgressStore = ProgressStoreFactory(api, database).create()

    @OptIn(ExperimentalStoreApi::class)
    fun clear() {
        io.launch {
            libraryStore.clear()
            itemStore.clear()
            progressStore.clear()
        }
    }
}