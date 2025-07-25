package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.response.AudioBookmark
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.BookmarkEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookmarkRepo @Inject constructor(db: MyDatabase) {

  private val queries = db.bookmarkEntityQueries

  fun byLibraryItemId(libraryItemId: String) =
    queries.byLibraryItemId(libraryItemId).executeAsList()

  suspend fun saveAndConvert(user: User): List<BookmarkEntity> {
    val entities = user.bookmarks.map { toEntity(it) }
    withContext(Dispatchers.IO) { cleanup(entities) }
    entities.forEach { entity -> queries.insert(entity) }
    return entities
  }

  fun insertAndConvert(audioBookmark: AudioBookmark): BookmarkEntity {
    val entity = toEntity(audioBookmark)
    queries.insert(entity)
    return entity
  }

  fun delete(libraryItemId: String, time: Long) = queries.delete(libraryItemId, time)

  fun updateTitle(libraryItemId: String, time: Long, title: String) =
    queries.updateTitle(title, libraryItemId, time)

  private fun cleanup(entities: List<BookmarkEntity>) {
    queries.transaction {
      val ids = queries.allIds().executeAsList()
      val newIds = entities.map { it.id }
      val toDelete = ids.filter { !newIds.contains(it) }
      toDelete.forEach { queries.deleteById(it) }
    }
  }

  private fun toEntity(audioBookmark: AudioBookmark): BookmarkEntity =
    BookmarkEntity(
      id = "${audioBookmark.libraryItemId}:${audioBookmark.time}",
      libraryItemId = audioBookmark.libraryItemId,
      title = audioBookmark.title,
      time = audioBookmark.time.toLong(),
      createdAt = audioBookmark.createdAt,
    )
}
