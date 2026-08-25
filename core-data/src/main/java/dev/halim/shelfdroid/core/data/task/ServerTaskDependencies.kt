package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.TasksResponse
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import javax.inject.Inject

/** Narrow network seam used by the task reducer and its deterministic tests. */
interface ServerTaskApi {
  suspend fun tasks(): Result<TasksResponse>

  suspend fun scanLibrary(libraryId: String): Result<Unit>
}

class ApiServerTaskApi @Inject constructor(private val api: ApiService) : ServerTaskApi {
  override suspend fun tasks(): Result<TasksResponse> = api.tasks()

  override suspend fun scanLibrary(libraryId: String): Result<Unit> = api.scanLibrary(libraryId)
}

/** Catalog synchronization is intentionally separate from task state and can be retried. */
interface ServerTaskCatalogSynchronizer {
  suspend fun synchronize(): Result<Unit>
}

class LibraryDataServerTaskCatalogSynchronizer
@Inject
constructor(private val libraryDataRepository: LibraryDataRepository) : ServerTaskCatalogSynchronizer {
  override suspend fun synchronize(): Result<Unit> {
    val result = libraryDataRepository.synchronize()
    return if (result.isSuccess) {
      Result.success(Unit)
    } else {
      Result.failure(result.error ?: IllegalStateException())
    }
  }
}

fun interface ServerTaskClock {
  fun now(): Long
}

class SystemServerTaskClock @Inject constructor() : ServerTaskClock {
  override fun now(): Long = System.currentTimeMillis()
}
