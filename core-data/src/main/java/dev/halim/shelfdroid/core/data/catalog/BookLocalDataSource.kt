package dev.halim.shelfdroid.core.data.catalog

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.database.BookEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class BookLocalDataSource @Inject constructor(db: MyDatabase, private val json: Json) {

  private val queries = db.bookEntityQueries

  fun byId(libraryItemId: String): Book? {
    return queries
      .byLibraryItemId(libraryItemId)
      .executeAsOneOrNull()
      ?.let { entity -> json.decodeFromString(entity.media) }
  }

  fun flowById(libraryItemId: String): Flow<Book?> {
    return queries
      .byLibraryItemId(libraryItemId)
      .asFlow()
      .mapToOneOrNull(Dispatchers.IO)
      .map { entity -> entity?.let { json.decodeFromString(it.media) } }
  }

  fun insert(libraryItemId: String, book: Book) {
    queries.insert(BookEntity(libraryItemId = libraryItemId, media = json.encodeToString(book)))
  }

  fun deleteById(libraryItemId: String) {
    queries.deleteByLibraryItemId(libraryItemId)
  }
}
