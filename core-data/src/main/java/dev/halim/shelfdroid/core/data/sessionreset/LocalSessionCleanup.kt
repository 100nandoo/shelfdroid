package dev.halim.shelfdroid.core.data.sessionreset

import dev.halim.shelfdroid.core.database.SessionDatabaseCleanup
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject

class LocalSessionCleanup
internal constructor(
  private val clearCurrentPlayback: suspend () -> Unit,
  private val clearTransientDownloads: () -> Unit,
  private val resetLocalAppPreferences: suspend () -> Unit,
  private val clearSessionScopedDatabase: () -> Unit,
  private val clearAppStorage: () -> Unit,
) {
  @Inject
  constructor(
    currentPlaybackCleanup: CurrentPlaybackCleanup,
    downloadRepo: DownloadRepo,
    dataStoreManager: DataStoreManager,
    sessionDatabaseCleanup: SessionDatabaseCleanup,
    appStorageCleanup: AppStorageCleanup,
  ) : this(
    clearCurrentPlayback = currentPlaybackCleanup::clearCurrentPlayback,
    clearTransientDownloads = downloadRepo::clearTransientDownloads,
    resetLocalAppPreferences = dataStoreManager::clear,
    clearSessionScopedDatabase = sessionDatabaseCleanup::clearSessionScopedData,
    clearAppStorage = appStorageCleanup::clear,
  )

  suspend fun clear(): Result<Unit> = runCatching {
    clearCurrentPlayback()
    clearTransientDownloads()
    resetLocalAppPreferences()
    clearSessionScopedDatabase()
    clearAppStorage()
  }
}
