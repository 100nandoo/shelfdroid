package dev.halim.shelfdroid.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.db.SqlDriver
import dev.halim.shelfdroid.network.Library
import dev.halim.shelfdroid.network.LibrarySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = ShelfdroidDb(databaseDriverFactory.createDriver())
    private val dbQuery = database.shelfdroidDbQueries

    internal fun libraries(): Flow<List<LibraryEntity>> {
        return dbQuery.selectAllLibraries().asFlow().mapToList(Dispatchers.IO)
    }

    internal fun get(id: String): Flow<LibraryEntity> {
        return dbQuery.selectLibraryById(id).asFlow().mapToOneNotNull(Dispatchers.IO)
    }

    internal fun remove(id: String) {
        dbQuery.removeLibraryById(id)
    }

    internal fun removeAll() {
        dbQuery.removeAllLibraries()
    }

    internal fun add(library: LibraryEntity) {
        remove(library.id)
        dbQuery.insertLibrary(
            library.id, library.name, library.displayOrder, library.icon, library.mediaType,
            library.provider, library.createdAt, library.lastUpdate
        )
    }

    internal fun addAll(libraries: List<LibraryEntity>) {
        removeAll()
        libraries.forEach { library -> add(library) }
    }

    private fun mapToLibraries(
        id: String, name: String, displayOrder: Long, icon: String?,
        mediaType: String, provider: String, createdAt: Long, lastUpdate: Long,
    ): Library {
        return Library(
            id, name, emptyList(), displayOrder.toInt(), icon ?: "", mediaType,
            provider, LibrarySettings(), createdAt, lastUpdate
        )
    }
}