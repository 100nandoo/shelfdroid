package dev.halim.shelfdroid.core.data.library

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

@Singleton
class LibraryDataRepository
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
  private var inFlight: Deferred<LibraryDataSyncResult>? = null

  suspend fun synchronize(): LibraryDataSyncResult {
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

  private suspend fun synchronizeOnce(): LibraryDataSyncResult {
    return try {
      val libraries = refreshLibraries()
      if (libraries.isFailure) {
        return LibraryDataSyncResult(libraries = libraries, items = null)
      }

      LibraryDataSyncResult(
        libraries = libraries,
        items = refreshLibraryItems(),
      )
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      LibraryDataSyncResult(libraries = Result.failure(error), items = null)
    }
  }
}
