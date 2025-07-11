package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.response.AudioBookmark
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.BookmarkEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject

class BookmarkRepo @Inject constructor(db: MyDatabase) {

  private val queries = db.bookmarkEntityQueries

  fun byLibraryItemId(libraryItemId: String) =
    queries.byLibraryItemId(libraryItemId).executeAsList()

  fun saveAndConvert(user: User): List<BookmarkEntity> {
    val entities = user.bookmarks.map { toEntity(it) }
    entities.forEach { entity -> queries.insert(entity) }
    return entities
  }

  private fun toEntity(audioBookmark: AudioBookmark): BookmarkEntity =
    BookmarkEntity(
      libraryItemId = audioBookmark.libraryItemId,
      title = audioBookmark.title,
      time = audioBookmark.time.toLong(),
      createdAt = audioBookmark.createdAt,
    )
}
