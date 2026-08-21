package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.request.RenameTagRequest
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

class TagApiServiceTest {
  private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

  @Test
  fun renameTag_usesServerRenameContract() = runTest {
    var request: Request? = null
    val service = apiService { request = it }

    val result = service.renameTag(RenameTagRequest(tag = "old", newTag = "new"))

    val captured = requireNotNull(request)
    assertTrue(result.isSuccess)
    assertEquals("POST", captured.method)
    assertEquals("/api/tags/rename", captured.url.encodedPath)
    assertEquals("{\"tag\":\"old\",\"newTag\":\"new\"}", captured.body!!.bodyToString())
  }

  @Test
  fun deleteTag_keepsPreEscapedBase64PathSafe() = runTest {
    var request: Request? = null
    val service = apiService { request = it }

    val result = service.deleteTag("AAA%2B")

    val captured = requireNotNull(request)
    assertTrue(result.isSuccess)
    assertEquals("DELETE", captured.method)
    assertEquals("/api/tags/AAA%2B", captured.url.encodedPath)
  }

  @Test
  fun tagRequests_preserveServerUrlSubpath() = runTest {
    var request: Request? = null
    val service = apiService(baseUrl = "https://audiobookshelf.example/shelf/") { request = it }

    val result = service.tags()

    assertTrue(result.isSuccess)
    assertEquals("/shelf/api/tags", requireNotNull(request).url.encodedPath)
  }

  private fun apiService(
    baseUrl: String = "https://audiobookshelf.example/",
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
            .body("{\"numItemsUpdated\":4,\"tagMerged\":true}".toResponseBody("application/json".toMediaType()))
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
