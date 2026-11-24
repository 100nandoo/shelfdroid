package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.response.login.LoginResponse
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.listening.BookmarkRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.database.SessionDatabaseCleanup
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.prefs.ServerAccessMode
import dev.halim.shelfdroid.core.prefs.ServerPrefs
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

interface LoginSuccessHandler {
  suspend fun onLoginSuccess(
    server: String,
    serverAccessMode: ServerAccessMode,
    response: LoginResponse,
  )
}

class DefaultLoginSuccessHandler
@Inject
constructor(
  private val mapper: LoginMapper,
  private val dataStoreManager: DataStoreManager,
  private val progressRepo: ProgressRepository,
  private val bookmarkRepo: BookmarkRepository,
  private val catalogSync: LibraryDataRepository,
  private val sessionDatabaseCleanup: SessionDatabaseCleanup,
) : LoginSuccessHandler {

  override suspend fun onLoginSuccess(
    server: String,
    serverAccessMode: ServerAccessMode,
    response: LoginResponse,
  ) {
    val userPrefs = mapper.toUserPrefs(response.user)
    val previousUser = dataStoreManager.userPrefs.first()
    val previousServer = dataStoreManager.baseUrl.first()
    // An OpenID provider can return a different user even during Forced re-login.
    if (
      previousUser.id.isNotBlank() && (previousUser.id != userPrefs.id || previousServer != server)
    ) {
      catalogSync.resetSession {
        dataStoreManager.updateUserPrefs(previousUser.copy(accessToken = "", refreshToken = ""))
        withContext(Dispatchers.IO) { sessionDatabaseCleanup.clearSessionScopedData() }
        dataStoreManager.updateCatalogSyncTime(0)
      }
    }

    dataStoreManager.updateBaseUrl(server)
    dataStoreManager.updateServerPrefs(
      ServerPrefs(
        version = response.serverSettings.version,
        logLevel = response.serverSettings.logLevel,
        accessMode = serverAccessMode,
      )
    )
    dataStoreManager.completeLogin(userPrefs)
    progressRepo.replaceUserProgress(response.user)
    bookmarkRepo.replaceUserBookmarks(response.user)
  }
}
