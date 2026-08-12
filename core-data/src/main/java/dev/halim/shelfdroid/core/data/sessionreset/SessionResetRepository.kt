package dev.halim.shelfdroid.core.data.sessionreset

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class LocalSessionCleanup
internal constructor(
  private val clearDatabase: () -> Unit,
  private val clearAppStorage: () -> Unit,
) {
  @Inject
  constructor(
    localDatabaseCleanup: LocalDatabaseCleanup,
    appStorageCleanup: AppStorageCleanup,
  ) : this(
    clearDatabase = localDatabaseCleanup::clear,
    clearAppStorage = appStorageCleanup::clear,
  )

  suspend fun clear(): Result<Unit> =
    runCatching {
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
