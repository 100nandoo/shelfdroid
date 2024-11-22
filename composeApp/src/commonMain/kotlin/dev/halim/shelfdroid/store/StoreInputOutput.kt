package dev.halim.shelfdroid.store


sealed class StoreOutput<T> {
    data class Single<T>(val data: T) : StoreOutput<T>()
    data class Collection<T>(val data: List<T>) : StoreOutput<T>()
}