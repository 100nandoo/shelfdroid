package dev.halim.shelfdroid.core.data

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.shelfdroid.core.database.ProgressDao
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ProgressRepo
@Inject
constructor(private val api: ApiService, private val progressDao: ProgressDao) {

  suspend fun saveMediaProgress(user: User) {
    val entities = user.mediaProgress.map { it.toEntity() }
    progressDao.insert(*entities.toTypedArray())
  }

  fun entities(): Flow<List<ProgressEntity>> {
    return flow {
        emit(progressDao.all())
        val response = api.me()
        response.onSuccess { user ->
          saveMediaProgress(user)
          emit(progressDao.all())
        }
      }
      .flowOn(Dispatchers.IO)
  }
}

fun MediaProgress.toEntity(): ProgressEntity {
  return ProgressEntity(
    id = this.id,
    libraryItemId = this.libraryItemId,
    episodeId = this.episodeId,
    mediaItemType = this.mediaItemType,
    progress = this.progress,
    duration = this.duration,
    currentTime = this.currentTime,
  )
}
