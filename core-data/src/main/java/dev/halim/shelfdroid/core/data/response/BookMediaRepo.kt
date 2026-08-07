package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.core.network.response.libraryitem.EbookFile
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.shelfdroid.core.database.BookEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookMediaRepo @Inject constructor(db: MyDatabase) {

  private val queries = db.bookEntityQueries

  fun byId(libraryItemId: String): Book? =
    queries.byLibraryItemId(libraryItemId).executeAsOneOrNull()?.let(::toBook)

  fun flowById(libraryItemId: String): Flow<Book?> =
    queries
      .byLibraryItemId(libraryItemId)
      .asFlow()
      .mapToOneOrNull(Dispatchers.IO)
      .map { entity -> entity?.let(::toBook) }

  fun insert(libraryItemId: String, book: Book) {
    val metadata = book.metadata
    val ebook = book.ebookFile
    val ebookMetadata = ebook?.metadata
    queries.insert(
      BookEntity(
        libraryItemId = libraryItemId,
        coverPath = book.coverPath,
        title = metadata.title,
        language = metadata.language,
        explicit = metadata.explicit.asLong(),
        description = metadata.description,
        subtitle = metadata.subtitle,
        publishedYear = metadata.publishedYear,
        publishedDate = metadata.publishedDate,
        publisher = metadata.publisher,
        isbn = metadata.isbn,
        asin = metadata.asin,
        descriptionPlain = metadata.descriptionPlain,
        duration = book.duration,
        ebookIno = ebook?.ino,
        ebookFilename = ebookMetadata?.filename,
        ebookExt = ebookMetadata?.ext,
        ebookPath = ebookMetadata?.path,
        ebookRelPath = ebookMetadata?.relPath,
        ebookSize = ebookMetadata?.size?.toLong(),
        ebookMtimeMs = ebookMetadata?.mtimeMs,
        ebookCtimeMs = ebookMetadata?.ctimeMs,
        ebookBirthtimeMs = ebookMetadata?.birthtimeMs,
        ebookFormat = ebook?.ebookFormat,
        ebookAddedAt = ebook?.addedAt,
        ebookUpdatedAt = ebook?.updatedAt,
      ),
    )
  }

  fun deleteById(libraryItemId: String) {
    queries.deleteByLibraryItemId(libraryItemId)
  }

  private fun toBook(entity: BookEntity): Book =
    Book(
      libraryItemId = entity.libraryItemId,
      coverPath = entity.coverPath,
      metadata =
        BookMetadata(
          title = entity.title,
          language = entity.language,
          explicit = entity.explicit.toBoolean(),
          description = entity.description,
          subtitle = entity.subtitle,
          publishedYear = entity.publishedYear,
          publishedDate = entity.publishedDate,
          publisher = entity.publisher,
          isbn = entity.isbn,
          asin = entity.asin,
          descriptionPlain = entity.descriptionPlain,
        ),
      ebookFile = entity.toEbookFile(),
      duration = entity.duration,
    )

  private fun BookEntity.toEbookFile(): EbookFile? {
    val ino = ebookIno ?: return null
    return EbookFile(
      ino = ino,
      metadata =
        FileMetadata(
          filename = ebookFilename.orEmpty(),
          ext = ebookExt.orEmpty(),
          path = ebookPath.orEmpty(),
          relPath = ebookRelPath.orEmpty(),
          size = ebookSize?.toInt() ?: 0,
          mtimeMs = ebookMtimeMs ?: 0,
          ctimeMs = ebookCtimeMs ?: 0,
          birthtimeMs = ebookBirthtimeMs ?: 0,
        ),
      ebookFormat = ebookFormat.orEmpty(),
      addedAt = ebookAddedAt ?: 0,
      updatedAt = ebookUpdatedAt ?: 0,
    )
  }
}

private fun Boolean.asLong() = if (this) 1L else 0L

private fun Long.toBoolean() = this == 1L
