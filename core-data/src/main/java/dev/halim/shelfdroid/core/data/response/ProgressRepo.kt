package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProgressRepo @Inject constructor(db: MyDatabase) {

  private val queries = db.progressEntityQueries
  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun byLibraryItemId(id: String): List<ProgressEntity> =
    queries.byLibraryItemId(id).executeAsList()

  fun bookById(id: String): ProgressEntity? = queries.bookById(id).executeAsOneOrNull()

  fun flowBookById(id: String): Flow<ProgressEntity?> =
    queries.bookById(id).asFlow().mapToOneOrNull(Dispatchers.IO)

  fun episodeById(id: String): ProgressEntity? = queries.episodeById(id).executeAsOneOrNull()

  fun flowEpisodeById(id: String): Flow<ProgressEntity?> =
    queries.episodeById(id).asFlow().mapToOneOrNull(Dispatchers.IO)

  suspend fun saveAndConvert(user: User): List<ProgressEntity> {
    val entities = user.mediaProgress.map(::toEntity)
    withContext(Dispatchers.IO) { cleanup(entities) }
    entities.forEach { entity -> queries.insert(entity) }
    return entities
  }

  fun entities(): List<ProgressEntity> {
    return queries.all().executeAsList()
  }

  fun flowEntities(): Flow<List<ProgressEntity>> {
    return queries.all().asFlow().mapToList(Dispatchers.IO)
  }

  fun updateMediaById(episodeId: String): Boolean =
    queries.toggleIsFinishedByEpisodeId(episodeId).value == 1L

  fun updateProgress(entity: ProgressEntity) {
    val episodeId = entity.episodeId
    if (episodeId.isNullOrBlank()) {
      queries.updateBookProgress(entity.progress, entity.currentTime, entity.libraryItemId)
    } else {
      queries.updatePodcastProgress(entity.progress, entity.currentTime, episodeId)
    }
  }

  private fun cleanup(entities: List<ProgressEntity>) {
    queries.transaction {
      val ids = queries.allIds().executeAsList()
      val newIds = entities.map { it.id }
      val toDelete = ids.filter { !newIds.contains(it) }
      toDelete.forEach { queries.deleteById(it) }
    }
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
    )
}
