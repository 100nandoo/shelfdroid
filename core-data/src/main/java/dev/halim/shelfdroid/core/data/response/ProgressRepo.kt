package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.Progress
import dev.halim.shelfdroid.core.database.ProgressQueries
import javax.inject.Inject

class ProgressRepo
@Inject
constructor(private val api: ApiService, private val progressQueries: ProgressQueries) {

  fun saveAndConvert(user: User): List<Progress> {
    val entities = user.mediaProgress.map { toEntity(it) }
    entities.forEach { progress -> progressQueries.insert(progress) }
    return entities
  }

  suspend fun entities(): List<Progress> {
    val response = api.me().getOrNull()
    return if (response != null) {
      saveAndConvert(response)
    } else {
      progressQueries.all().executeAsList()
    }
  }

  fun toEntity(mediaProgress: MediaProgress): Progress =
    Progress(
      id = mediaProgress.id,
      libraryItemId = mediaProgress.libraryItemId,
      episodeId = mediaProgress.episodeId,
      mediaItemType = mediaProgress.mediaItemType,
      progress = mediaProgress.progress.toDouble(),
      duration = mediaProgress.duration.toDouble(),
      currentTime = mediaProgress.currentTime.toDouble(),
    )
}
