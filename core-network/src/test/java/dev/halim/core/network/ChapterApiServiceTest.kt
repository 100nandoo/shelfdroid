package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.request.UpdateLibraryItemChaptersRequest
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

class ChapterApiServiceTest {

  @Test
  fun updateLibraryItemChapters_usesAudiobookshelfChapterContract() = runTest {
    var request: Request? = null
    val service = apiService { request = it }

    val result =
      service.updateLibraryItemChapters(
        "book-1",
        UpdateLibraryItemChaptersRequest(
          chapters =
            listOf(
              UpdateLibraryItemChaptersRequest.Chapter(
                id = 0,
                title = "Intro",
                start = 0.0,
                end = 12.5,
              )
            )
        ),
      )

    val captured = requireNotNull(request)
    assertTrue(result.isSuccess)
    assertEquals("POST", captured.method)
    assertEquals("/api/items/book-1/chapters", captured.url.encodedPath)
    assertEquals(
      "{\"chapters\":[{\"id\":0,\"start\":0.0,\"end\":12.5,\"title\":\"Intro\"}]}",
      captured.body!!.bodyToString(),
    )
  }

  private fun apiService(onRequest: (Request) -> Unit): ApiService {
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
            .body("{\"success\":true,\"updated\":true}".toResponseBody("application/json".toMediaType()))
            .build()
        }
        .build()
    return Retrofit.Builder()
      .baseUrl("https://audiobookshelf.example/")
      .client(client)
      .addConverterFactory(
        Json { explicitNulls = false }.asConverterFactory("application/json".toMediaType())
      )
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
