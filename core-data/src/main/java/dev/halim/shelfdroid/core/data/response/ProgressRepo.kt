package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.core.database.ProgressEntityQueries
import javax.inject.Inject

class ProgressRepo
@Inject
constructor(private val api: ApiService, private val queries: ProgressEntityQueries) {

  fun byLibraryItemId(id: String): ProgressEntity? {
    return queries.byLibraryItemId(id).executeAsOneOrNull()
  }

  fun saveAndConvert(user: User): List<ProgressEntity> {
    val entities = user.mediaProgress.map { toEntity(it) }
    entities.forEach { progress -> queries.insert(progress) }
    return entities
  }

  suspend fun entities(): List<ProgressEntity> {
    val response = api.me().getOrNull()
    return if (response != null) {
      saveAndConvert(response)
    } else {
      queries.all().executeAsList()
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
    )
}
