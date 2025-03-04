package dev.halim.shelfdroid.core.data

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.LoginRequest
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager) {

    suspend fun login(server: String, username: String, password: String): Result<Unit> {
        DataStoreManager.BASE_URL = server
        val request = LoginRequest(username, password)
        val result = api.login(request)
        result.onSuccess { response ->
                DataStoreManager.BASE_URL = server
                dataStoreManager.updateToken(response.user.token)
                dataStoreManager.updateUserId(response.user.id)
                dataStoreManager.generateDeviceId()

            return Result.success(Unit)
        }
        result.onFailure { error -> return Result.failure(error) }
        return Result.failure(Exception("Login failed"))
    }
}
