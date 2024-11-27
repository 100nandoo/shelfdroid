package dev.halim.shelfdroid.store

import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

suspend fun <Key : Any, Output : Any> Store<Key, Output>.cachedAndRefresh(key: Key) =
    stream(StoreReadRequest.cached(key, true))
        .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
        .first()
        .requireData()

suspend fun <Key : Any, Output : Any> Store<Key, Output>.cached(key: Key) =
    stream(StoreReadRequest.cached(key, refresh = false))
        .filterNot { it is StoreReadResponse.Loading || it is StoreReadResponse.NoNewData }
        .first()
        .requireData()