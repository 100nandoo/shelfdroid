package dev.halim.shelfdroid.db

import app.cash.sqldelight.db.SqlDriver
import dev.halim.shelfdroid.db.dao.ItemDao
import dev.halim.shelfdroid.db.dao.LibraryDao
import dev.halim.shelfdroid.db.dao.ProgressDao

interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

class Database(databaseDriverFactory: DatabaseDriverFactory, adapter: DatabaseAdapter) {
    private val database = ShelfdroidDb(
        databaseDriverFactory.createDriver(),
        LibraryEntityAdapter = LibraryEntity.Adapter(adapter.listOfStringsAdapter),
        ItemEntityAdapter = ItemEntity.Adapter(adapter.listOfBookChaptersAdapter)
    )
    val libraryDao = LibraryDao(database.libraryQueries)
    val itemDao = ItemDao(database.itemQueries)
    val progressDao = ProgressDao(database.progressQueries)
}