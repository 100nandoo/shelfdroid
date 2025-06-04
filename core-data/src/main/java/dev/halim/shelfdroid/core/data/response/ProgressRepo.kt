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
    val entities = user.mediaProgress.map { it.toEntity() }
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
}

fun MediaProgress.toEntity(): Progress {
  return Progress(
    id = this.id,
    libraryItemId = this.libraryItemId,
    episodeId = this.episodeId,
    mediaItemType = this.mediaItemType,
    progress = this.progress.toDouble(),
    duration = this.duration.toDouble(),
    currentTime = this.currentTime.toDouble(),
  )
}
