package dev.halim.shelfdroid.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class LibraryDao(private val libraryQuery: LibraryQueries) {
    fun addLibrary(libraryEntity: LibraryEntity) {
        libraryQuery.insert(
            libraryEntity.id,
            libraryEntity.name,
            libraryEntity.displayOrder,
            libraryEntity.icon,
            libraryEntity.mediaType,
            libraryEntity.provider,
            libraryEntity.createdAt,
            libraryEntity.lastUpdate,
            libraryEntity.itemIds
        )
    }

    fun addAllLibrary(list: List<LibraryEntity>) {
        removeAllLibrary()
        list.forEach { addLibrary(it) }
    }

    fun getLibrary(id: String): Flow<LibraryEntity> {
        return libraryQuery.selectById(id).asFlow().mapToOneNotNull(Dispatchers.IO)
    }

    fun allLibrary(): Flow<List<LibraryEntity>> {
        return libraryQuery.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    fun removeLibrary(id: String) {
        libraryQuery.removeById(id)
    }

    fun removeAllLibrary() {
        libraryQuery.removeAll()
    }
}