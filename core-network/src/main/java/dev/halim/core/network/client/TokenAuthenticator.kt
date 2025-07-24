package dev.halim.core.network.client

import dagger.Lazy
import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator
@Inject
constructor(
  private val apiService: Lazy<ApiService>,
  private val dataStoreManager: DataStoreManager,
) : Authenticator {

  override fun authenticate(route: Route?, response: Response): Request? {
    synchronized(this) {
      return runBlocking {
        val refreshToken =
          dataStoreManager.userPrefs.firstOrNull()?.refreshToken ?: return@runBlocking null

        val refreshResponse = apiService.get().refresh(refreshToken).getOrNull() ?: return@runBlocking null

        val newTokens = refreshResponse.user.accessToken
        dataStoreManager.updateAccessToken(refreshResponse.user.accessToken)
        dataStoreManager.updateRefreshToken(refreshResponse.user.refreshToken)

        response.request.newBuilder().header("Authorization", "Bearer $newTokens").build()
      }
    }
  }
}
