package dev.halim.core.network.client

import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import okio.IOException

class HostSelectionInterceptor @Inject constructor(private val dataStoreManager: DataStoreManager) :
  Interceptor {

  init {
    DataStoreManager.BASE_URL = dataStoreManager.baseUrl()
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val isAnonymousRequest = request.tag(AnonymousRequestTag::class.java) != null
    val requestBuilder = request.newBuilder()
    if (request.url.host != AudiobookshelfBaseUrl.DEFAULT.host) {
      return chain.proceed(requestBuilder.build())
    }

    val baseUrl =
      AudiobookshelfBaseUrl.parse(DataStoreManager.BASE_URL)
        ?: return chain.proceed(requestBuilder.build())
    val newUrl =
      baseUrl.resolveEncoded(request.url.encodedPath, request.url.encodedQuery).toHttpUrlOrNull()
        ?: throw IOException("Host is invalid.")

    requestBuilder.url(newUrl)
    val token = dataStoreManager.accessToken()
    if (!isAnonymousRequest && token.isNotBlank()) {
      requestBuilder.header("Authorization", "Bearer $token")
    }
    return chain.proceed(requestBuilder.build())
  }
}
