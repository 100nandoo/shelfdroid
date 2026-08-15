package dev.halim.shelfdroid.core.data.catalog

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CatalogSyncResult(
  val libraries: Result<Unit>,
  val items: LibraryItemRefreshResult?,
) {
  val isSuccess: Boolean
    get() = libraries.isSuccess && items?.isSuccess == true

  val error: Throwable?
    get() = libraries.exceptionOrNull() ?: items?.failures?.firstOrNull()?.error
}

data class LibraryItemRefreshResult(
  val refreshedLibraryIds: Set<String>,
  val failures: List<LibraryItemRefreshFailure>,
) {
  val isSuccess: Boolean
    get() = failures.isEmpty()
}

data class LibraryItemRefreshFailure(val libraryId: String, val error: Throwable)

@Singleton
class CatalogSynchronizer
internal constructor(
  private val refreshLibraries: suspend () -> Result<Unit>,
  private val refreshLibraryItems: suspend () -> LibraryItemRefreshResult,
  private val scope: CoroutineScope,
) {

  @Inject
  constructor(
    libraryRepository: LibraryRepository,
    libraryItemRepository: LibraryItemRepository,
    @Named("io") scope: CoroutineScope,
  ) : this(
    refreshLibraries = libraryRepository::refreshLibraries,
    refreshLibraryItems = libraryItemRepository::refreshLibraryItems,
    scope = scope,
  )

  private val mutex = Mutex()
  private var inFlight: Deferred<CatalogSyncResult>? = null

  suspend fun synchronize(): CatalogSyncResult {
    val refresh = mutex.withLock {
      inFlight?.takeUnless { it.isCompleted }
        ?: scope.async(start = CoroutineStart.LAZY) { synchronizeOnce() }.also { inFlight = it }
    }

    return try {
      refresh.await()
    } finally {
      mutex.withLock {
        if (inFlight === refresh && refresh.isCompleted) {
          inFlight = null
        }
      }
    }
  }

  private suspend fun synchronizeOnce(): CatalogSyncResult {
    return try {
      val libraries = refreshLibraries()
      if (libraries.isFailure) {
        return CatalogSyncResult(libraries = libraries, items = null)
      }

      CatalogSyncResult(
        libraries = libraries,
        items = refreshLibraryItems(),
      )
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      CatalogSyncResult(libraries = Result.failure(error), items = null)
    }
  }
}
