package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.response.BatchLibraryItemsResponse
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.LibraryItemEntityQueries
import javax.inject.Inject
import kotlinx.serialization.json.Json

class LibraryItemRepo
@Inject
constructor(
  private val api: ApiService,
  private val queries: LibraryItemEntityQueries,
  private val helper: Helper,
  private val json: Json,
) {

  fun saveAndConvert(
    libraryId: String,
    response: BatchLibraryItemsResponse,
  ): List<LibraryItemEntity> {
    val entities = response.libraryItems.map { toEntity(it, libraryId) }
    entities.forEach { entity -> queries.insert(entity) }
    return entities
  }

  suspend fun idsByLibraryId(libraryId: String): List<String> {
    val result = api.libraryItems(libraryId).getOrNull()
    val ids = result?.results?.map { it.id }
    return ids ?: queries.idsByLibraryId(libraryId).executeAsList()
  }

  suspend fun entities(libraryId: String, ids: List<String>): List<LibraryItemEntity> {
    val result = api.batchLibraryItems(BatchLibraryItemsRequest(ids)).getOrNull()

    return if (result != null) {
      saveAndConvert(libraryId, result)
    } else {
      queries.byLibraryId(libraryId).executeAsList()
    }
  }

  fun toEntity(item: LibraryItem, libraryId: String): LibraryItemEntity {
    val media = item.media
    return if (media is Book) {
      LibraryItemEntity(
        id = item.id,
        libraryId = libraryId,
        inoId = media.audioFiles.first().ino,
        title = media.metadata.title ?: "",
        author = media.metadata.authors.joinToString { it.name },
        cover = helper.generateItemCoverUrl(item.id),
        isBook = 1,
        media = json.encodeToString(media),
      )
    } else {
      media as Podcast
      LibraryItemEntity(
        id = item.id,
        libraryId = libraryId,
        inoId = "",
        title = media.metadata.title ?: "",
        author = media.metadata.author ?: "",
        cover = helper.generateItemCoverUrl(item.id),
        isBook = 0,
        media = json.encodeToString(media),
      )
    }
  }
}
