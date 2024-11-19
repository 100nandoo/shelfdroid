package dev.halim.shelfdroid.db

import app.cash.sqldelight.db.SqlDriver
import dev.halim.shelfdroid.network.Library
import dev.halim.shelfdroid.network.LibrarySettings

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = ShelfdroidDb(databaseDriverFactory.createDriver())
    private val dbQuery = database.shelfdroidDbQueries

    internal fun libraries(): List<Library> {
        return dbQuery.selectAllLibraries(::mapToLibraries).executeAsList()
    }

    internal fun clear() {
        dbQuery.removeAllLibraries()
    }

    internal fun add(library: Library) {
        dbQuery.insertLibrary(
            library.id, library.name, library.displayOrder.toLong(), library.icon, library.mediaType,
            library.provider, library.createdAt, library.lastUpdate
        )
    }

    internal fun addAll(libraries: List<Library>) {
        clear()
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