package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.core.database.ProgressEntityQueries
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ProgressRepo
@Inject
constructor(private val api: ApiService, private val queries: ProgressEntityQueries) {

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  fun byLibraryItemId(id: String): ProgressEntity? =
    queries.byLibraryItemId(id).executeAsOneOrNull()

  fun bookById(id: String): ProgressEntity? = queries.bookById(id).executeAsOneOrNull()

  fun episodeById(id: String): ProgressEntity? = queries.episodeById(id).executeAsOneOrNull()

  fun saveAndConvert(user: User): List<ProgressEntity> {
    val entities = user.mediaProgress.map(::toEntity)

    repositoryScope.launch {
      queries.transaction {
        entities.forEach { progress ->
          when (progress.mediaItemType) {
            "book" -> queries.deleteBookById(progress.libraryItemId)
            else -> queries.deleteEpisodeById(progress.episodeId)
          }
          queries.insert(progress)
        }
      }
    }
    return entities
  }

  suspend fun entities(): List<ProgressEntity> {
    return api.me().getOrNull()?.let { saveAndConvert(it) } ?: queries.all().executeAsList()
  }

  fun updateMediaById(episodeId: String): Boolean =
    queries.toggleIsFinishedByEpisodeId(episodeId).value == 1L

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
