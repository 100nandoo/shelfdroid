package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.TasksResponse
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import javax.inject.Inject

/** Narrow network seam used by the task reducer and its deterministic tests. */
interface TaskApi {
  suspend fun tasks(): Result<TasksResponse>

  suspend fun scanLibrary(libraryId: String): Result<Unit>

  suspend fun matchLibrary(libraryId: String): Result<Unit>
}

class ApiTaskApi @Inject constructor(private val api: ApiService) : TaskApi {
  override suspend fun tasks(): Result<TasksResponse> = api.tasks()

  override suspend fun scanLibrary(libraryId: String): Result<Unit> = api.scanLibrary(libraryId)

  override suspend fun matchLibrary(libraryId: String): Result<Unit> = api.matchLibrary(libraryId)
}

/** Catalog synchronization is intentionally separate from task state and can be retried. */
interface TaskCatalogSynchronizer {
  suspend fun synchronize(): Result<Unit>
}

class LibraryDataTaskCatalogSynchronizer
@Inject
constructor(private val libraryDataRepository: LibraryDataRepository) : TaskCatalogSynchronizer {
  override suspend fun synchronize(): Result<Unit> {
    val result = libraryDataRepository.synchronize()
    return if (result.isSuccess) {
      Result.success(Unit)
    } else {
      Result.failure(result.error ?: IllegalStateException())
    }
  }
}

fun interface TaskClock {
  fun now(): Long
}

class SystemTaskClock @Inject constructor() : TaskClock {
  override fun now(): Long = System.currentTimeMillis()
}
