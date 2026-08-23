package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.request.metadata.CreateCustomMetadataProviderRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class CustomMetadataProviderApiServiceTest {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Test
  fun customProviderRequests_preserveServerUrlSubpath() = runTest {
    val requests = mutableListOf<Request>()
    val service = apiService("https://audiobookshelf.example/shelf/") { requests += it }

    assertTrue(service.customMetadataProviders().isSuccess)
    assertTrue(
      service
        .createCustomMetadataProvider(
          CreateCustomMetadataProviderRequest(
            name = "Community",
            url = "https://provider.example",
            authHeaderValue = "Bearer secret",
          )
        )
        .isSuccess
    )
    assertTrue(service.deleteCustomMetadataProvider("provider-1").isSuccess)

    assertEquals(
      listOf(
        "/shelf/api/custom-metadata-providers",
        "/shelf/api/custom-metadata-providers",
        "/shelf/api/custom-metadata-providers/provider-1",
      ),
      requests.map { it.url.encodedPath },
    )
    assertEquals(
      "{\"name\":\"Community\",\"url\":\"https://provider.example\",\"mediaType\":\"book\",\"authHeaderValue\":\"Bearer secret\"}",
      requests[1].body!!.bodyToString(),
    )
  }

  private fun apiService(
    baseUrl: String,
    onRequest: (Request) -> Unit,
  ): ApiService {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val request = chain.request()
          onRequest(request)
          Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(
              "{\"provider\":{\"id\":\"provider-1\",\"name\":\"Community\",\"url\":\"https://provider.example\",\"mediaType\":\"book\"}}"
                .toResponseBody("application/json".toMediaType())
            )
            .build()
        }
        .build()
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .addCallAdapterFactory(ResultCallAdapterFactory.create())
      .build()
      .create(ApiService::class.java)
  }

  private fun okhttp3.RequestBody.bodyToString(): String {
    val buffer = okio.Buffer()
    writeTo(buffer)
    return buffer.readUtf8()
  }
}
