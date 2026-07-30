package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.response.LoginResponse
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
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
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
) : LoginSuccessHandler {

  override suspend fun onLoginSuccess(server: String, response: LoginResponse) {
    dataStoreManager.apply {
      val userPrefs = mapper.toUserPrefs(response.user)
      updateBaseUrl(server)
      completeLogin(userPrefs)
    }
    progressRepo.saveAndConvert(response.user)
    bookmarkRepo.saveAndConvert(response.user)
  }
}
