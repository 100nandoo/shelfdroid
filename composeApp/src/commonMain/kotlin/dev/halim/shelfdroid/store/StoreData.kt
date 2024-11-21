package dev.halim.shelfdroid.store

sealed class StoreData<T> {
    data class Single<T>(val data: T) : StoreData<T>()
    data class Collection<T>(val data: List<T>) : StoreData<T>()
}
