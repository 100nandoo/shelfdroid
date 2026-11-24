package dev.halim.shelfdroid.core.data.sessionreset

import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.database.SessionDatabaseCleanup
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject

class LocalSessionCleanup
internal constructor(
  private val clearCurrentPlayback: () -> Unit,
  private val clearTransientDownloads: () -> Unit,
  private val resetLocalAppPreferences: suspend () -> Unit,
  private val clearSessionScopedDatabase: () -> Unit,
  private val clearAppStorage: () -> Unit,
  private val resetCatalogSession: suspend (suspend () -> Unit) -> Unit = { clear -> clear() },
) {
  @Inject
  constructor(
    currentPlaybackCleanup: CurrentPlaybackCleanup,
    downloadRepo: DownloadRepo,
    dataStoreManager: DataStoreManager,
    sessionDatabaseCleanup: SessionDatabaseCleanup,
    appStorageCleanup: AppStorageCleanup,
    libraryDataRepository: LibraryDataRepository,
  ) : this(
    clearCurrentPlayback = currentPlaybackCleanup::clearCurrentPlayback,
    clearTransientDownloads = downloadRepo::clearTransientDownloads,
    resetLocalAppPreferences = dataStoreManager::clear,
    clearSessionScopedDatabase = sessionDatabaseCleanup::clearSessionScopedData,
    clearAppStorage = appStorageCleanup::clear,
    resetCatalogSession = libraryDataRepository::resetSession,
  )

  suspend fun clear(): Result<Unit> = runCatching {
    clearCurrentPlayback()
    clearTransientDownloads()
    resetCatalogSession {
      resetLocalAppPreferences()
      clearSessionScopedDatabase()
    }
    clearAppStorage()
  }
}
