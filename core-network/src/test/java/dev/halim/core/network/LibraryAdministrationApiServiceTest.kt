package dev.halim.core.network

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.request.CreateLibraryRequest
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
