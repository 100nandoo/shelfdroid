package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.Progress
import dev.halim.shelfdroid.core.database.ProgressQueries
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProgressRepo
@Inject
constructor(private val api: ApiService, private val progressQueries: ProgressQueries) {

  fun saveMediaProgress(user: User) {
    val entitiesNew = user.mediaProgress.map { it.toEntityNew() }
    entitiesNew.forEach { progress -> progressQueries.insert(progress) }
  }

  fun entities(): Flow<List<Progress>> =
    flow {
        emit(progressQueries.all().executeAsList())

        val response = api.me()
        response.onSuccess { user ->
          saveMediaProgress(user)
          emit(progressQueries.all().executeAsList())
        }
      }
      .flowOn(Dispatchers.IO)
}

fun MediaProgress.toEntityNew(): Progress {
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
