package dev.halim.core.network.client

import dagger.Lazy
import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.AuthPromptReason
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
    val path = response.request.url.encodedPath
    if (path.contains("login") || path.contains("refresh")) {
      return null
    }
    if (response.responseCount() >= 2) {
      return runBlocking { beginForcedReLogin() }
    }
    synchronized(this) {
      return runBlocking {
        val refreshToken =
          dataStoreManager.userPrefs.firstOrNull()?.refreshToken
            ?: return@runBlocking beginForcedReLogin()

        if (refreshToken.isBlank()) {
          return@runBlocking beginForcedReLogin()
        }

        val refreshResponse =
          apiService.get().refresh(refreshToken).getOrNull()
            ?: return@runBlocking beginForcedReLogin()

        val newTokens = refreshResponse.user
        dataStoreManager.updateTokens(
          accessToken = newTokens.accessToken,
          refreshToken = newTokens.refreshToken,
        )

        response.request
          .newBuilder()
          .header("Authorization", "Bearer ${newTokens.accessToken}")
          .build()
      }
    }
  }

  private suspend fun beginForcedReLogin(): Request? {
    dataStoreManager.beginForcedReLogin(AuthPromptReason.RefreshFailed)
    return null
  }
}

private fun Response.responseCount(): Int {
  var result = 1
  var priorResponse = priorResponse
  while (priorResponse != null) {
    result++
    priorResponse = priorResponse.priorResponse
  }
  return result
}
