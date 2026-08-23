package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.request.RenameGenreRequest
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

class GenreApiServiceTest {
  private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
  }

  @Test
  fun renameGenre_usesServerRenameContract() = runTest {
    var request: Request? = null
    val service = apiService { request = it }

    val result = service.renameGenre(RenameGenreRequest(genre = "old", newGenre = "new"))

    val captured = requireNotNull(request)
    assertTrue(result.isSuccess)
    assertEquals("POST", captured.method)
    assertEquals("/api/genres/rename", captured.url.encodedPath)
    assertEquals("{\"genre\":\"old\",\"newGenre\":\"new\"}", captured.body!!.bodyToString())
  }

  @Test
  fun deleteGenre_keepsPreEscapedBase64PathSafe() = runTest {
    var request: Request? = null
    val service = apiService { request = it }

    val result = service.deleteGenre("5pel5pys")

    val captured = requireNotNull(request)
    assertTrue(result.isSuccess)
    assertEquals("DELETE", captured.method)
    assertEquals("/api/genres/5pel5pys", captured.url.encodedPath)
  }

  @Test
  fun genreRequests_preserveServerUrlSubpath() = runTest {
    var request: Request? = null
    val service = apiService(baseUrl = "https://audiobookshelf.example/shelf/") { request = it }

    val result = service.genres()

    assertTrue(result.isSuccess)
    assertEquals("/shelf/api/genres", requireNotNull(request).url.encodedPath)
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
            .body(
              "{\"numItemsUpdated\":4,\"genreMerged\":true}"
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
