package dev.halim.shelfdroid.db

import app.cash.sqldelight.db.SqlDriver
import dev.halim.shelfdroid.db.dao.ItemDao
import dev.halim.shelfdroid.db.dao.LibraryDao

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = ShelfdroidDb(
        databaseDriverFactory.createDriver(),
        LibraryEntityAdapter = LibraryEntity.Adapter(listOfStringsAdapter)
    )
    val libraryDao = LibraryDao(database.libraryQueries)
    val itemDao = ItemDao(database.itemQueries)
}