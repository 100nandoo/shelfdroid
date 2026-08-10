package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.response.login.LoginResponse
import dev.halim.shelfdroid.core.data.listening.BookmarkRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject

interface LoginSuccessHandler {
  suspend fun onLoginSuccess(server: String, response: LoginResponse)
}

class DefaultLoginSuccessHandler
@Inject
constructor(
  private val mapper: LoginMapper,
  private val dataStoreManager: DataStoreManager,
  private val progressRepo: ProgressRepository,
  private val bookmarkRepo: BookmarkRepository,
) : LoginSuccessHandler {

  override suspend fun onLoginSuccess(server: String, response: LoginResponse) {
    dataStoreManager.apply {
      val userPrefs = mapper.toUserPrefs(response.user)
      updateBaseUrl(server)
      completeLogin(userPrefs)
    }
    progressRepo.replaceUserProgress(response.user)
    bookmarkRepo.replaceUserBookmarks(response.user)
  }
}
