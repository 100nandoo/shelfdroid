package dev.halim.shelfdroid.core.data.sessionreset

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class LocalSessionCleanup
internal constructor(
  private val clearCurrentPlayback: () -> Unit,
  private val clearTransientDownloads: () -> Unit,
  private val resetLocalAppPreferences: suspend () -> Unit,
  private val clearDatabase: () -> Unit,
  private val clearAppStorage: () -> Unit,
) {
  @Inject
  constructor(
    currentPlaybackCleanup: CurrentPlaybackCleanup,
    downloadRepo: DownloadRepo,
    dataStoreManager: DataStoreManager,
    localDatabaseCleanup: LocalDatabaseCleanup,
    appStorageCleanup: AppStorageCleanup,
  ) : this(
    clearCurrentPlayback = currentPlaybackCleanup::clearCurrentPlayback,
    clearTransientDownloads = downloadRepo::clearTransientDownloads,
    resetLocalAppPreferences = dataStoreManager::clear,
    clearDatabase = localDatabaseCleanup::clear,
    clearAppStorage = appStorageCleanup::clear,
  )

  suspend fun clear(): Result<Unit> =
    runCatching {
      clearCurrentPlayback()
      clearTransientDownloads()
      resetLocalAppPreferences()
      clearDatabase()
      clearAppStorage()
    }
}

class SessionResetRepository
internal constructor(
  private val refreshToken: suspend () -> String,
  private val remoteLogout: suspend (String) -> Result<Unit>,
  private val localCleanup: suspend () -> Result<Unit>,
) {
  @Inject
  constructor(
    api: ApiService,
    prefsRepository: PrefsRepository,
    localSessionCleanup: LocalSessionCleanup,
  ) : this(
    refreshToken = { prefsRepository.userPrefs.first().refreshToken },
    remoteLogout = { token -> api.logout(token).map {} },
    localCleanup = localSessionCleanup::clear,
  )

  suspend fun fullLogout(): Result<Unit> {
    val currentRefreshToken = refreshToken()
    if (currentRefreshToken.isBlank()) {
      return Result.failure(
        IllegalStateException(
          "Unable to log out because the current session is missing its refresh token."
        )
      )
    }

    val remoteLogoutResult = remoteLogout(currentRefreshToken)
    remoteLogoutResult.exceptionOrNull()?.let { return Result.failure(it) }

    return localCleanup()
  }

  suspend fun logoutForAccountSwitch(): Result<Unit> {
    val currentRefreshToken = refreshToken()
    if (currentRefreshToken.isNotBlank()) {
      remoteLogout(currentRefreshToken)
    }

    return localCleanup()
  }
}
