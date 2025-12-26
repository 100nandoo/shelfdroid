package dev.halim.core.network.client

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException

class HostSelectionInterceptor @Inject constructor(private val dataStoreManager: DataStoreManager) :
  Interceptor {

  init {
    DataStoreManager.BASE_URL = runBlocking {
      dataStoreManager.baseUrl.firstOrNull() ?: "https://www.audiobookshelf.org"
    }
  }

  override fun intercept(chain: Interceptor.Chain): Response {

    var request = chain.request()
    if (request.url.host.contains("mzstatic.com")) {
      return chain.proceed(request)
    }

    val host: String = DataStoreManager.BASE_URL

    val result = runCatching { request.url.newBuilder().host(host).build() }
    if (result.isSuccess) {
      val newUrl = result.getOrThrow()
      val token = runBlocking { dataStoreManager.userPrefs.firstOrNull()?.accessToken ?: "" }
      if (token.isBlank().not()) {
        request = request.newBuilder().header("Authorization", "Bearer $token").build()
      }
      request = request.newBuilder().url(newUrl).build()

      return chain.proceed(request)
    } else {
      handleHostError(result)
    }
    return chain.proceed(request)
  }

  private fun handleHostError(result: Result<HttpUrl>) {
    val e = result.exceptionOrNull()
    if (e != null && e is IllegalArgumentException) {
      throw IOException("Host is invalid.")
    } else throw IOException(e?.message)
  }
}
