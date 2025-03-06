package dev.halim.core.network.interceptor

import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class HostSelectionInterceptor @Inject constructor(private val dataStoreManager: DataStoreManager) : Interceptor {

    init {
        DataStoreManager.BASE_URL = runBlocking {
            dataStoreManager.baseUrl.firstOrNull() ?: "https://www.audiobookshelf.org"
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {

        var request = chain.request()
        val host: String = DataStoreManager.BASE_URL

        val newUrl = request.url.newBuilder()
            .host(host)
            .build()

        val token = runBlocking { dataStoreManager.token.firstOrNull() ?: "" }
        if (token.isBlank().not()) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        request = request.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(request)
    }

}