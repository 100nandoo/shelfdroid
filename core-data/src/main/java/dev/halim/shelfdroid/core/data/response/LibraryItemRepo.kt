package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.request.OpenItemRssFeedMetadataDetails
import dev.halim.core.network.request.OpenItemRssFeedRequest
import dev.halim.core.network.response.BatchLibraryItemsResponse
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.RssFeed
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedDetails
import dev.halim.shelfdroid.core.database.LibraryItemCatalog
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.download.BookCleanupRequest
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
  private val bookMediaRepo: BookMediaRepo,
  private val podcastMediaRepo: PodcastMediaRepo,
  private val podcastEpisodeRepo: PodcastEpisodeRepo,
) {

  private val queries = db.libraryItemEntityQueries
  private val libraryQueries = db.libraryEntityQueries

  suspend fun remote() {
    val libraryIds = libraryQueries.allIds().executeAsList()
    coroutineScope {
      libraryIds
        .map { libraryId ->
          async {
            val ids = idsByLibraryId(libraryId)
            if (ids.isEmpty()) return@async
            val result = api.batchLibraryItems(BatchLibraryItemsRequest(ids)).getOrNull()

            if (result != null) {
              val items = result.libraryItems
              val entities = convert(libraryId, result)
              cleanupPodcasts(libraryId, items)
              val booksToDelete = cleanupBooks(libraryId, entities)
              queries.transaction {
                booksToDelete.forEach { entity ->
                  deleteInTransaction(entity.id)
                }
                items.forEach { item -> insertInTransaction(item, libraryId) }
              }
            }
          }
        }
        .awaitAll()
    }
  }

  fun createPodcast(libraryItem: LibraryItem, libraryId: String) {
    insert(libraryItem, libraryId)
  }

  fun updateItem(item: LibraryItem) {
    insert(item, item.libraryId)
  }

  suspend fun refreshItem(id: String, include: String? = null): Result<LibraryItem> {
    val result = api.item(id, include = include)
    result.getOrNull()?.let(::updateItem)
    return result
  }

  suspend fun openGeneratedRssFeedForItem(
    itemId: String,
    details: GeneratedRssFeedDetails,
  ): Result<RssFeed> {
    val request =
      OpenItemRssFeedRequest(
        serverAddress = currentWebBaseUrl(),
        slug = details.slug,
        metadataDetails =
          OpenItemRssFeedMetadataDetails(
            preventIndexing = details.preventIndexing,
            ownerName = details.ownerName,
            ownerEmail = details.ownerEmail,
          ),
      )
    val response =
      api.openItemRssFeed(itemId, request).getOrElse {
        return Result.failure(it)
      }
    if (refreshItem(itemId, include = "rssfeed").isFailure) {
      patchRssFeed(itemId, response.feed)
    }
    return Result.success(response.feed)
  }

  suspend fun closeGeneratedRssFeedForItem(itemId: String, feedId: String): Result<Unit> {
    api.closeRssFeed(feedId).getOrElse {
      return Result.failure(it)
    }
    if (refreshItem(itemId, include = "rssfeed").isFailure) {
      patchRssFeed(itemId, null)
    }
    return Result.success(Unit)
  }

  fun currentWebBaseUrlForUi(): String = currentWebBaseUrl()

  fun byId(id: String): LibraryItemEntity? {
    return queries.byId(id).executeAsOneOrNull()
  }

  fun flowById(id: String): Flow<LibraryItemEntity?> {
    return queries.byId(id).asFlow().mapToOneOrNull(Dispatchers.IO)
  }

  fun bookById(id: String): Book? {
    return bookMediaRepo.byId(id)
  }

  fun flowBookById(id: String): Flow<Book?> = bookMediaRepo.flowById(id)

  fun podcastById(id: String): Podcast? = podcastMediaRepo.byId(id)

  suspend fun idsByLibraryId(libraryId: String): List<String> {
    val result = api.libraryItems(libraryId).getOrNull()
    val ids = result?.results?.map { it.id }
    return ids ?: queries.idsByLibraryId(libraryId).executeAsList()
  }

  fun flowCatalog(): Flow<Map<String, List<LibraryItemCatalog>>> {
    return queries.libraryItemCatalog().asFlow().mapToList(Dispatchers.IO).map { list ->
      list.groupBy { it.libraryId }
    }
  }

  fun podcastInfoList(libraryId: String): List<PodcastInfo> {
    return queries.podcastsByLibraryId(libraryId).executeAsList().map { entity ->
      val metadata = podcastMediaRepo.byId(entity.id)?.metadata
      PodcastInfo(
        id = entity.id,
        itunesId = metadata?.itunesId.orEmpty(),
        artist = metadata?.author?.ifBlank { null } ?: entity.author,
        title = metadata?.title?.ifBlank { null } ?: entity.title,
        feedUrl = metadata?.feedUrl.orEmpty(),
      )
    }
  }

  suspend fun cleanupItem(id: String) {
    val entity = queries.byId(id).executeAsOneOrNull()
    if (entity?.isBook == 1L) {
      val book = bookById(id)
      if (book != null) {
        downloadRepo.deleteBook(
          title = entity.title,
          author = entity.author,
          tracks =
            book.audioTracks.map { track ->
              dev.halim.shelfdroid.core.DownloadUiState(
                id =
                  if (book.audioTracks.size == 1) entity.id
                  else helper.generateDownloadId(entity.id, track.index.toString()),
                filename = track.metadata.filename,
              )
            },
        )
      }
    } else {
      downloadRepo.delete(id)
    }
    queries.transaction {
      deleteInTransaction(id)
    }
    progressRepo.deleteItem(id)
  }

  fun deleteEpisodes(id: String, episodeIds: Set<String>) {
    if (episodeIds.isEmpty()) return
    podcastEpisodeRepo.deleteByIds(episodeIds)

    downloadRepo.cleanupEpisode(episodeIds.toList())
  }

  private fun convert(
    libraryId: String,
    response: BatchLibraryItemsResponse,
  ): List<LibraryItemEntity> {
    val entities = response.libraryItems.map { toEntity(it, libraryId) }
    return entities
  }

  private suspend fun cleanupBooks(
    libraryId: String,
    entities: List<LibraryItemEntity>,
  ): List<LibraryItemEntity> {
    val existingEntities = queries.byLibraryId(libraryId).executeAsList().filter { it.isBook == 1L }
    val newIds = entities.map { it.id }.toSet()
    val toDelete = existingEntities.filter { it.id !in newIds }
    if (toDelete.isEmpty()) return emptyList()

    downloadRepo.cleanupBooks(
      toDelete.mapNotNull { entity ->
        val book = bookById(entity.id) ?: return@mapNotNull null
        BookCleanupRequest(
          itemId = entity.id,
          title = entity.title,
          author = entity.author,
          filenames = book.audioTracks.map { it.metadata.filename },
        )
      }
    )

    return toDelete
  }

  private fun cleanupPodcasts(libraryId: String, items: List<LibraryItem>) {
    val existingEpisodeIds =
      queries
        .byLibraryId(libraryId)
        .executeAsList()
        .filter { it.isBook == 0L }
        .flatMap { entity -> podcastEpisodeRepo.byLibraryItemId(entity.id) }
        .map { it.id }
        .toSet()

    downloadRepo.cleanupEpisode(stalePodcastEpisodeIds(existingEpisodeIds, items))
  }

  private fun toEntity(item: LibraryItem, libraryId: String): LibraryItemEntity {
    val media = item.media
    return if (media is Book) {
      LibraryItemEntity(
        id = item.id,
        libraryId = libraryId,
        inoId = media.primaryInoId(),
        title = media.metadata.title ?: "",
        description = media.metadata.description ?: "",
        author = media.metadata.authors.joinToString { it.name },
        cover = helper.generateItemCoverUrl(item.id, item.updatedAt),
        updatedAt = item.updatedAt,
        duration = helper.formatDuration(media.duration ?: 0.0),
        isBook = 1,
        rssFeed = item.rssFeed?.let(json::encodeToString),
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
        rssFeed = item.rssFeed?.let(json::encodeToString),
        addedAt = item.addedAt,
      )
    }
  }

  private fun insert(item: LibraryItem, libraryId: String) {
    queries.transaction {
      insertInTransaction(item, libraryId)
    }
  }

  private fun insertInTransaction(item: LibraryItem, libraryId: String) {
    val media = item.media
    queries.insert(toEntity(item, libraryId))
    if (media is Book) {
      bookMediaRepo.insert(item.id, media)
      podcastMediaRepo.deleteById(item.id)
      podcastEpisodeRepo.deleteByLibraryItemId(item.id)
    } else {
      media as Podcast
      bookMediaRepo.deleteById(item.id)
      podcastMediaRepo.insert(item.id, media)
      podcastEpisodeRepo.replace(item.id, media.episodes)
    }
  }

  private fun deleteInTransaction(id: String) {
    queries.deleteById(id)
    bookMediaRepo.deleteById(id)
    podcastMediaRepo.deleteById(id)
    podcastEpisodeRepo.deleteByLibraryItemId(id)
  }

  private fun patchRssFeed(itemId: String, feed: RssFeed?) {
    val entity = byId(itemId) ?: return
    queries.insert(entity.copy(rssFeed = feed?.let(json::encodeToString)))
  }

  private fun currentWebBaseUrl(): String =
    AudiobookshelfBaseUrl.parse(DataStoreManager.BASE_URL)?.value
      ?: AudiobookshelfBaseUrl.DEFAULT_VALUE
}

internal fun stalePodcastEpisodeIds(
  existingEpisodeIds: Set<String>,
  items: List<LibraryItem>,
): List<String> {
  val newEpisodeIds =
    items
      .mapNotNull { item -> (item.media as? Podcast)?.episodes }
      .flatten()
      .map { episode -> episode.id }
      .toSet()
  return (existingEpisodeIds - newEpisodeIds).toList()
}

internal fun Book.primaryInoId(): String = audioFiles.firstOrNull()?.ino.orEmpty()

data class PodcastInfo(
  val id: String,
  val itunesId: String?,
  val title: String,
  val artist: String,
  val feedUrl: String,
)
