package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.request.CreateLibraryRequest
import dev.halim.core.network.request.ReorderLibraryRequest
import dev.halim.core.network.request.ValidateCronRequest
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

class LibraryAdministrationApiServiceTest {
  private val json = Json { explicitNulls = false }

  @Test
  fun createLibrary_serializesAudiobookshelfPayload() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result =
      service.createLibrary(
        CreateLibraryRequest(
          name = "Books",
          folders = listOf(CreateLibraryRequest.Folder("C:/Books")),
          mediaType = "book",
          icon = "audiobookshelf",
          provider = "audible",
        )
      )

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("POST", request.method)
    assertEquals("/api/libraries", request.url.encodedPath)
    assertEquals(
      "{\"name\":\"Books\",\"folders\":[{\"path\":\"C:/Books\"}],\"mediaType\":\"book\",\"icon\":\"audiobookshelf\",\"provider\":\"audible\"}",
      request.body!!.bodyToString(),
    )
  }

  @Test
  fun createLibrary_serializesBookSettingsWithoutPodcastOnlyFields() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    service.createLibrary(
      CreateLibraryRequest(
        name = "Books",
        folders = listOf(CreateLibraryRequest.Folder("/books")),
        mediaType = "book",
        icon = "audiobookshelf",
        provider = "audible",
        settings =
          CreateLibraryRequest.Settings(
            coverAspectRatio = 1,
            disableWatcher = false,
            audiobooksOnly = true,
            skipMatchingMediaWithAsin = true,
            skipMatchingMediaWithIsbn = false,
            epubsAllowScriptedContent = true,
            hideSingleBookSeries = true,
            onlyShowLaterBooksInContinueSeries = false,
            metadataPrecedence = listOf("folderStructure", "audioMetatags"),
            markAsFinishedTimeRemaining = 10,
          ),
      )
    )

    assertEquals(
      "{\"name\":\"Books\",\"folders\":[{\"path\":\"/books\"}],\"mediaType\":\"book\",\"icon\":\"audiobookshelf\",\"provider\":\"audible\",\"settings\":{\"coverAspectRatio\":1,\"disableWatcher\":false,\"audiobooksOnly\":true,\"skipMatchingMediaWithAsin\":true,\"skipMatchingMediaWithIsbn\":false,\"epubsAllowScriptedContent\":true,\"hideSingleBookSeries\":true,\"onlyShowLaterBooksInContinueSeries\":false,\"metadataPrecedence\":[\"folderStructure\",\"audioMetatags\"],\"markAsFinishedTimeRemaining\":10}}",
      requireNotNull(capturedRequest).body!!.bodyToString(),
    )
  }

  @Test
  fun createLibrary_serializesPodcastSettingsWithoutBookOnlyFields() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    service.createLibrary(
      CreateLibraryRequest(
        name = "Podcasts",
        folders = listOf(CreateLibraryRequest.Folder("/podcasts")),
        mediaType = "podcast",
        icon = "audiobookshelf",
        provider = "itunes",
        settings =
          CreateLibraryRequest.Settings(
            coverAspectRatio = 1,
            disableWatcher = false,
            podcastSearchRegion = "gb",
            markAsFinishedTimeRemaining = 10,
          ),
      )
    )

    val body = requireNotNull(capturedRequest).body!!.bodyToString()
    assertTrue(body.contains("\"podcastSearchRegion\":\"gb\""))
    assertTrue(!body.contains("audiobooksOnly"))
    assertTrue(!body.contains("metadataPrecedence"))
  }

  @Test
  fun createLibrary_serializesEnabledAutomaticScanSchedule() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    service.createLibrary(
      CreateLibraryRequest(
        name = "Books",
        folders = listOf(CreateLibraryRequest.Folder("/books")),
        mediaType = "book",
        icon = "audiobookshelf",
        provider = "audible",
        settings = CreateLibraryRequest.Settings(autoScanCronExpression = "0 0 * * 1"),
      )
    )

    assertTrue(
      requireNotNull(capturedRequest).body!!.bodyToString().contains(
        "\"autoScanCronExpression\":\"0 0 * * 1\""
      )
    )
  }

  @Test
  fun validateCron_postsExpressionToServerEndpoint() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result = service.validateCron(ValidateCronRequest("0 0 * * 1"))

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("POST", request.method)
    assertEquals("/api/validate-cron", request.url.encodedPath)
    assertEquals(
      "{\"expression\":\"0 0 * * 1\"}",
      request.body!!.bodyToString(),
    )
  }

  @Test
  fun reorderLibraries_postsCompleteOrderArray() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result =
      service.reorderLibraries(
        listOf(
          ReorderLibraryRequest(id = "podcasts", newOrder = 1),
          ReorderLibraryRequest(id = "books", newOrder = 2),
        )
      )

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("POST", request.method)
    assertEquals("/api/libraries/order", request.url.encodedPath)
    assertEquals(
      "[{\"id\":\"podcasts\",\"newOrder\":1},{\"id\":\"books\",\"newOrder\":2}]",
      request.body!!.bodyToString(),
    )
  }

  @Test
  fun scanLibrary_onlyAcceptsTheRequest() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result = service.scanLibrary("books")

    assertTrue(result.isSuccess)
    assertEquals("POST", requireNotNull(capturedRequest).method)
    assertEquals("/api/libraries/books/scan", requireNotNull(capturedRequest).url.encodedPath)
  }

  @Test
  fun matchLibrary_usesAudiobookshelfMatchAllEndpoint() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result = service.matchLibrary("books")

    assertTrue(result.isSuccess)
    val request = requireNotNull(capturedRequest)
    assertEquals("GET", request.method)
    assertEquals("/api/libraries/books/matchall", request.url.encodedPath)
  }

  @Test
  fun tasks_loadsOperationAgnosticServerTaskSnapshot() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result = service.tasks()

    assertTrue(result.isSuccess)
    val request = requireNotNull(capturedRequest)
    assertEquals("GET", request.method)
    assertEquals("/api/tasks", request.url.encodedPath)
  }

  @Test
  fun filesystem_serializesPathAndLevelAndMapsWindowsDirectories() = runTest {
    var capturedRequest: Request? = null
    val service = apiService { capturedRequest = it }

    val result = service.filesystem(path = "C:/", level = 1)

    val request = requireNotNull(capturedRequest)
    assertTrue(result.isSuccess)
    assertEquals("GET", request.method)
    assertEquals("/api/filesystem", request.url.encodedPath)
    assertEquals("C:/", request.url.queryParameter("path"))
    assertEquals("1", request.url.queryParameter("level"))
    assertEquals("C:/Media", result.getOrThrow().directories.single().path)
    assertEquals(false, result.getOrThrow().posix)
  }

  private fun apiService(onRequest: (Request) -> Unit): ApiService {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val request = chain.request()
          onRequest(request)
          val body =
            if (request.url.encodedPath == "/api/filesystem") {
              "{\"posix\":false,\"directories\":[{\"path\":\"C:/Media\",\"dirname\":\"Media\",\"level\":0}]}"
            } else if (request.url.encodedPath == "/api/tasks") {
              "{\"tasks\":[]}"
            } else if (request.url.encodedPath == "/api/libraries/order") {
              "{\"libraries\":[{\"id\":\"podcasts\",\"name\":\"Podcasts\",\"mediaType\":\"podcast\",\"displayOrder\":1},{\"id\":\"books\",\"name\":\"Books\",\"mediaType\":\"book\",\"displayOrder\":2}]}"
            } else {
              "{\"id\":\"library-1\",\"name\":\"Books\",\"mediaType\":\"book\"}"
            }
          Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
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

  private fun okhttp3.RequestBody.bodyToString(): String {
    val buffer = okio.Buffer()
    writeTo(buffer)
    return buffer.readUtf8()
  }
}
