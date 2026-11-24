package dev.halim.shelfdroid.core.data.library

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LibraryDataRepository
internal constructor(
  private val refreshLibraries: suspend () -> Result<Unit>,
  private val refreshLibraryItems: suspend () -> LibraryItemRefreshResult,
  private val scope: CoroutineScope,
  private val isSignedIn: suspend () -> Boolean = { true },
  private val lastSuccess: suspend () -> Long = { 0 },
  private val saveSuccess: suspend (Long) -> Unit = {},
  private val now: () -> Long = System::currentTimeMillis,
) {

  @Inject
  constructor(
    libraryRepository: LibraryRepository,
    libraryItemRepository: LibraryItemRepository,
    dataStore: DataStoreManager,
    @Named("io") scope: CoroutineScope,
  ) : this(
    refreshLibraries = libraryRepository::refreshLibraries,
    refreshLibraryItems = libraryItemRepository::refreshLibraryItems,
    scope = scope,
    isSignedIn = { dataStore.userPrefs.first().accessToken.isNotBlank() },
    lastSuccess = { dataStore.catalogSyncTime.first() },
    saveSuccess = { dataStore.updateCatalogSyncTime(it) },
  )

  private val mutex = Mutex()
  private var inFlight: Deferred<LibraryDataSyncResult>? = null
  @Volatile private var lastFailure: Long? = null

  suspend fun synchronize(): LibraryDataSyncResult {
    val refresh = mutex.withLock {
      if (!isSignedIn())
        return LibraryDataSyncResult(
          libraries =
            Result.failure(IllegalStateException("Catalog synchronization requires sign-in")),
          items = null,
        )
      activeRefresh() ?: startRefresh()
    }
    return awaitRefresh(refresh)
  }

  suspend fun synchronizeIfStale(): LibraryDataSyncResult? {
    val refresh = mutex.withLock {
      if (!isSignedIn()) return null
      activeRefresh()
        ?: run {
          val current = now()
          val success = lastSuccess()
          if (success > 0 && current - success in 0 until 15 * 60_000L) return null
          if (lastFailure?.let { current - it in 0 until 60_000L } == true) return null
          startRefresh()
        }
    }
    return awaitRefresh(refresh)
  }

  internal suspend fun resetSession(clear: suspend () -> Unit) = mutex.withLock {
    inFlight?.cancelAndJoin()
    inFlight = null
    lastFailure = null
    clear()
  }

  private fun activeRefresh(): Deferred<LibraryDataSyncResult>? = inFlight?.takeUnless {
    it.isCompleted
  }

  private fun startRefresh(): Deferred<LibraryDataSyncResult> =
    scope
      .async(start = CoroutineStart.LAZY) {
        val result = synchronizeOnce()
        if (result.isSuccess) {
          saveSuccess(now())
          lastFailure = null
        } else {
          lastFailure = now()
        }
        result
      }
      .also { inFlight = it }

  private suspend fun awaitRefresh(
    refresh: Deferred<LibraryDataSyncResult>
  ): LibraryDataSyncResult {
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
