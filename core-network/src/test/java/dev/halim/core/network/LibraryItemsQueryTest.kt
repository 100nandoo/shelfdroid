package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class LibraryItemsQueryTest {

  private val json = Json {
    coerceInputValues = true
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    explicitNulls = false
  }

  @Test
  fun libraryItems_whenTypedQueryProvided_serializesAudiobookshelfParameters() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { request -> capturedRequest = request }

    val result =
      service.libraryItems(
        libraryId = "library-1",
        query =
          LibraryItemsQuery(
            limit = 25,
            page = 3,
            minified = true,
            sort = LibraryItemsSort.Book.Title,
            desc = true,
          ),
      )

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("/api/libraries/library-1/items", request.url.encodedPath)
    assertEquals("25", request.url.queryParameter("limit"))
    assertEquals("3", request.url.queryParameter("page"))
    assertEquals("1", request.url.queryParameter("minified"))
    assertEquals("media.metadata.title", request.url.queryParameter("sort"))
    assertEquals("1", request.url.queryParameter("desc"))
  }

  @Test
  fun libraryItems_whenAscendingRequested_serializesDescAsZero() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { request -> capturedRequest = request }

    val result =
      service.libraryItems(
        libraryId = "library-1",
        query = LibraryItemsQuery(desc = false),
      )

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("0", request.url.queryParameter("desc"))
  }

  @Test
  fun libraryItems_whenPodcastSortProvided_serializesMappedWireValue() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { request -> capturedRequest = request }

    val result =
      service.libraryItems(
        libraryId = "library-1",
        query = LibraryItemsQuery(sort = LibraryItemsSort.Podcast.Author),
      )

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("media.metadata.author", request.url.queryParameter("sort"))
  }

  @Test
  fun libraryItems_whenTypedSortProvidedDirectly_serializesWireValue() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { request -> capturedRequest = request }

    val result =
      service.libraryItems(
        libraryId = "library-1",
        sort = LibraryItemsSort.Book.Title,
      )

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("media.metadata.title", request.url.queryParameter("sort"))
  }

  @Test
  fun libraryItems_whenOnlyLibraryIdProvided_omitsOptionalQueryParameters() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { request -> capturedRequest = request }

    val result = service.libraryItems("library-1")

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("/api/libraries/library-1/items", request.url.encodedPath)
    assertNull(request.url.query)
  }

  private fun apiService(onRequest: (Request) -> Unit): ApiService {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val request = chain.request()
          onRequest(request)
          okResponse(request)
        }
        .build()

    return Retrofit.Builder()
      .baseUrl("https://audiobookshelf.example/")
      .client(client)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .addCallAdapterFactory(ResultCallAdapterFactory.create())
      .build()
      .create(ApiService::class.java)
  }

  private fun okResponse(request: Request): Response {
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body("{\"results\":[]}".toResponseBody("application/json".toMediaType()))
      .build()
  }
}
