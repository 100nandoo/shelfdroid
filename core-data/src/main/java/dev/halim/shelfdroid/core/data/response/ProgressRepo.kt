package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProgressRepo @Inject constructor(db: MyDatabase, val helper: Helper) {

  private val queries = db.progressEntityQueries
  private val repoScope = CoroutineScope(Dispatchers.IO)

  fun flowAll(): Flow<List<ProgressEntity>> = queries.all().asFlow().mapToList(Dispatchers.IO)

  fun byLibraryItemId(id: String): List<ProgressEntity> =
    queries.byLibraryItemId(id).executeAsList()

  fun byLibraryItemIdAndFinished(id: String): List<ProgressEntity> =
    queries.byLibraryItemIdAndFinished(id).executeAsList()

  fun flowByLibraryItemId(id: String): Flow<List<ProgressEntity>> =
    queries.byLibraryItemId(id).asFlow().mapToList(Dispatchers.IO)

  fun bookById(id: String): ProgressEntity? = queries.bookById(id).executeAsOneOrNull()

  fun flowBookById(id: String): Flow<ProgressEntity?> =
    queries.bookById(id).asFlow().mapToOneOrNull(Dispatchers.IO)

  fun episodeById(id: String): ProgressEntity? = queries.episodeById(id).executeAsOneOrNull()

  fun flowEpisodeById(id: String): Flow<ProgressEntity?> =
    queries.episodeById(id).asFlow().mapToOneOrNull(Dispatchers.IO)

  fun saveAndConvert(user: User): List<ProgressEntity> {
    val entities = user.mediaProgress.map(::toEntity)
    repoScope.launch {
      cleanup()
      entities.forEach { entity -> queries.insert(entity) }
    }
    return entities
  }

  fun toggleIsFinishedByEpisodeId(episodeId: String): Boolean =
    queries.toggleIsFinishedByEpisodeId(episodeId).value == 1L

  fun markEpisodeFinished(libraryItemId: String, episodeId: String) {
    val now = helper.nowMilis()
    queries.markEpisodeFinished(libraryItemId, episodeId, now)
  }

  fun updateProgress(entity: ProgressEntity) {
    val now = helper.nowMilis()
    val episodeId = entity.episodeId
    if (episodeId.isNullOrBlank()) {
      queries.updateBookProgress(entity.progress, entity.currentTime, now, entity.libraryItemId)
    } else {
      queries.updatePodcastProgress(entity.progress, entity.currentTime, now, episodeId)
    }
  }

  fun finishedEpisodeIdsByLibraryItemId(libraryItemId: String): List<String> {
    return queries.finishedEpisodeIdsByLibraryItemId(libraryItemId).executeAsList()
  }

  fun finishedEpisodeIdsGroupedByLibraryItemId(): Flow<Map<String, List<String>>> {
    return queries
      .finishedEpisodeIdsGroupedByLibraryItemId()
      .asFlow()
      .mapToList(Dispatchers.IO)
      .map { list ->
        list.associate { it.libraryItemId to (it.finishedEpisodeIds?.split(",") ?: emptyList()) }
      }
  }

  fun deleteItem(itemId: String) {
    queries.deleteItemById(itemId)
  }

  private fun cleanup() {
    queries.deleteAll()
  }

  private fun toEntity(mediaProgress: MediaProgress): ProgressEntity =
    ProgressEntity(
      id = mediaProgress.id,
      libraryItemId = mediaProgress.libraryItemId,
      episodeId = mediaProgress.episodeId,
      mediaItemType = mediaProgress.mediaItemType,
      progress = mediaProgress.progress.toDouble(),
      duration = mediaProgress.duration.toDouble(),
      currentTime = mediaProgress.currentTime.toDouble(),
      isFinished = if (mediaProgress.isFinished) 1 else 0,
      lastUpdate = mediaProgress.lastUpdate,
    )
}
