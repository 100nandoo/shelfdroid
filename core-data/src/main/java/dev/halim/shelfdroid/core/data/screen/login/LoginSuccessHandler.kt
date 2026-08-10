package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.response.login.LoginResponse
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.ServerPrefs
import dev.halim.shelfdroid.core.data.listening.BookmarkRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject

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
) : LoginSuccessHandler {

  override suspend fun onLoginSuccess(
    server: String,
    serverAccessMode: ServerAccessMode,
    response: LoginResponse,
  ) {
    dataStoreManager.apply {
      val userPrefs = mapper.toUserPrefs(response.user)
      val serverPrefs =
        ServerPrefs(
          version = response.serverSettings.version,
          logLevel = response.serverSettings.logLevel,
          accessMode = serverAccessMode,
        )
      updateBaseUrl(server)
      updateServerPrefs(serverPrefs)
      completeLogin(userPrefs)
    }
    progressRepo.replaceUserProgress(response.user)
    bookmarkRepo.replaceUserBookmarks(response.user)
  }
}
