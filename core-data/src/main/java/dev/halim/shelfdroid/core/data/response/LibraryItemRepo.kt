package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.response.BatchLibraryItemsResponse
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class LibraryItemRepo
@Inject
constructor(
  private val api: ApiService,
  db: MyDatabase,
  private val helper: Helper,
  private val json: Json,
  private val downloadRepo: DownloadRepo,
) {

  private val repoScope = CoroutineScope(Dispatchers.IO)

  private val queries = db.libraryItemEntityQueries
  private val libraryQueries = db.libraryEntityQueries

  suspend fun remote() {
    val libraryIds = libraryQueries.allIds().executeAsList()
    coroutineScope {
      libraryIds.forEach { libraryId ->
        async {
          val ids = idsByLibraryId(libraryId)
          val result = api.batchLibraryItems(BatchLibraryItemsRequest(ids)).getOrNull()

          if (result != null) {
            val entities = convert(libraryId, result)
            repoScope.launch {
              cleanupDownloads(libraryId, entities)
              cleanup(libraryId, entities)
              entities.forEach { queries.insert(it) }
            }
          }
        }
      }
    }
  }

  fun byId(id: String): LibraryItemEntity? {
    return queries.byId(id).executeAsOneOrNull()
  }

  fun flowById(id: String): Flow<LibraryItemEntity?> {
    return queries.byId(id).asFlow().mapToOneOrNull(Dispatchers.IO)
  }

  suspend fun idsByLibraryId(libraryId: String): List<String> {
    val result = api.libraryItems(libraryId).getOrNull()
    val ids = result?.results?.map { it.id }
    return ids ?: queries.idsByLibraryId(libraryId).executeAsList()
  }

  fun flowEntities(): Flow<Map<String, List<LibraryItemEntity>>> {
    return queries.all().asFlow().mapToList(Dispatchers.IO).map { list ->
      list.groupBy { it.libraryId }
    }
  }

  private fun convert(
    libraryId: String,
    response: BatchLibraryItemsResponse,
  ): List<LibraryItemEntity> {
    val entities = response.libraryItems.map { toEntity(it, libraryId) }
    return entities
  }

  private fun cleanup(libraryId: String, entities: List<LibraryItemEntity>) {
    queries.transaction {
      val ids = queries.idsByLibraryId(libraryId).executeAsList()
      val newIds = entities.map { it.id }
      val toDelete = ids.filter { !newIds.contains(it) }

      downloadRepo.cleanupBook(toDelete)

      toDelete.forEach { queries.deleteById(it) }
    }
  }

  private fun cleanupDownloads(libraryId: String, entities: List<LibraryItemEntity>) {
    val episodeIds =
      queries
        .byLibraryId(libraryId)
        .executeAsList()
        .filter { it.isBook == 0L }
        .map { Json.decodeFromString<Podcast>(it.media) }
        .flatMap { it.episodes }
        .map { it.id }
    val newEpisodeIds =
      entities
        .filter { it.isBook == 0L }
        .map { Json.decodeFromString<Podcast>(it.media) }
        .flatMap { it.episodes }
        .map { it.id }

    val toDeleteEpisode = episodeIds.filter { !newEpisodeIds.contains(it) }
    downloadRepo.cleanupEpisode(toDeleteEpisode)
  }

  private fun toEntity(item: LibraryItem, libraryId: String): LibraryItemEntity {
    val media = item.media
    return if (media is Book) {
      LibraryItemEntity(
        id = item.id,
        libraryId = libraryId,
        inoId = media.audioFiles.first().ino,
        title = media.metadata.title ?: "",
        description = media.metadata.description ?: "",
        author = media.metadata.authors.joinToString { it.name },
        cover = helper.generateItemCoverUrl(item.id),
        duration = helper.formatDuration(media.duration ?: 0.0),
        isBook = 1,
        media = json.encodeToString(media),
        addedAt = item.addedAt,
      )
    } else {
      media as Podcast
      LibraryItemEntity(
        id = item.id,
        libraryId = libraryId,
        inoId = "",
        title = media.metadata.title ?: "",
        description = media.metadata.description ?: "",
        author = media.metadata.author ?: "",
        cover = helper.generateItemCoverUrl(item.id),
        duration = "",
        isBook = 0,
        media = json.encodeToString(media),
        addedAt = item.addedAt,
      )
    }
  }
}
