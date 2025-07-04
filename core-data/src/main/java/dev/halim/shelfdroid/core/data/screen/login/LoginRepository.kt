package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.LoginResponse
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject

class LoginRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  private val progressRepo: ProgressRepo,
) {

  suspend fun login(server: String, username: String, password: String): Result<Unit> {
    DataStoreManager.BASE_URL = server
    val request = LoginRequest(username, password)
    val result = api.login(request)
    result.onSuccess { response ->
      updateDataStoreManager(server, response)
      progressRepo.saveAndConvert(response.user)
      return Result.success(Unit)
    }
    result.onFailure { error ->
      return Result.failure(error)
    }
    return Result.failure(Exception("Login failed"))
  }

  private suspend fun updateDataStoreManager(server: String, response: LoginResponse) {
    dataStoreManager.apply {
      updateBaseUrl(server)
      updateToken(response.user.token)
      updateUserId(response.user.id)
    }
  }
}
