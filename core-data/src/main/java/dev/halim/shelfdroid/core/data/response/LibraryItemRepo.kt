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
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.download.BookCleanupRequest
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
  private val progressRepo: ProgressRepo,
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
              cleanupPodcasts(libraryId, entities)
              cleanupBooks(libraryId, entities)
              entities.forEach { queries.insert(it) }
            }
          }
        }
      }
    }
  }

  fun createPodcast(libraryItem: LibraryItem, libraryId: String) {
    val entity = toEntity(libraryItem, libraryId)
    queries.insert(entity)
  }

  fun updateItem(item: LibraryItem) {
    queries.insert(toEntity(item, item.libraryId))
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

  fun podcastInfoList(libraryId: String): List<PodcastInfo> {
    return queries.podcastsByLibraryId(libraryId).executeAsList().map { entity ->
      val podcast = Json.decodeFromString<Podcast>(entity.media)
      val metadata = podcast.metadata
      PodcastInfo(
        id = entity.id,
        itunesId = metadata.itunesId,
        artist = podcast.metadata.author ?: "",
        title = podcast.metadata.title ?: "",
        feedUrl = metadata.feedUrl ?: "",
      )
    }
  }

  fun cleanupItem(id: String) {
    val entity = queries.byId(id).executeAsOneOrNull()
    if (entity?.isBook == 1L) {
      val book = Json.decodeFromString<Book>(entity.media)
      downloadRepo.deleteBook(
        title = entity.title,
        author = entity.author,
        tracks =
          book.audioTracks.map { track ->
            dev.halim.shelfdroid.core.DownloadUiState(
              id = if (book.audioTracks.size == 1) entity.id else helper.generateDownloadId(entity.id, track.index.toString()),
              filename = track.metadata.filename,
            )
          },
      )
    } else {
      downloadRepo.delete(id)
    }
    queries.deleteById(id)
    progressRepo.deleteItem(id)
  }

  fun deleteEpisodes(id: String, episodeIds: Set<String>) {
    val entity = queries.byId(id).executeAsOne()

    val podcast = Json.decodeFromString<Podcast>(entity.media)

    val updatedPodcast = podcast.copy(episodes = podcast.episodes.filterNot { it.id in episodeIds })

    queries.updateMediaById(media = json.encodeToString(updatedPodcast), id = id)

    downloadRepo.cleanupEpisode(episodeIds.toList())
  }

  private fun convert(
    libraryId: String,
    response: BatchLibraryItemsResponse,
  ): List<LibraryItemEntity> {
    val entities = response.libraryItems.map { toEntity(it, libraryId) }
    return entities
  }

  private fun cleanupBooks(libraryId: String, entities: List<LibraryItemEntity>) {
    queries.transaction {
      val existingEntities = queries.byLibraryId(libraryId).executeAsList().filter { it.isBook == 1L }
      val newIds = entities.map { it.id }.toSet()
      val toDelete = existingEntities.filter { it.id !in newIds }

      downloadRepo.cleanupBooks(
        toDelete.map { entity ->
          val book = Json.decodeFromString<Book>(entity.media)
          BookCleanupRequest(
            itemId = entity.id,
            title = entity.title,
            author = entity.author,
            filenames = book.audioTracks.map { it.metadata.filename },
          )
        }
      )

      toDelete.forEach { queries.deleteById(it.id) }
    }
  }

  private fun cleanupPodcasts(libraryId: String, entities: List<LibraryItemEntity>) {
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
        .filter { it.isBook.toBoolean().not() }
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
        cover = helper.generateItemCoverUrl(item.id, item.updatedAt),
        updatedAt = item.updatedAt,
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
        cover = helper.generateItemCoverUrl(item.id, item.updatedAt),
        updatedAt = item.updatedAt,
        duration = "",
        isBook = 0,
        media = json.encodeToString(media),
        addedAt = item.addedAt,
      )
    }
  }
}

data class PodcastInfo(
  val id: String,
  val itunesId: Int?,
  val title: String,
  val artist: String,
  val feedUrl: String,
)
