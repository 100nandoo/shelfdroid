package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.metadata.genre.GenreMutation
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationError
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationException
import dev.halim.shelfdroid.core.data.metadata.tag.TagMutation

import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.ApiService
import dev.halim.core.network.client.HostSelectionInterceptor
import dev.halim.core.network.client.SessionCookieJar
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.tags.TagRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MetadataUtilsRepositoryHttpTest {

  @Test
  fun load_admin_sortsTagsAndUpdatesAdministrativeCache() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses = listOf(Stub(200, "{\"tags\":[\"zeta\",\"Alpha\",\"beta\"]}")),
      )
    try {
      val result = fixture.repository.loadTags()

      assertEquals(listOf("Alpha", "beta", "zeta"), result.getOrThrow())
      assertEquals(listOf("Alpha", "beta", "zeta"), fixture.tagRepository.listTags())
      assertEquals(listOf("GET /api/tags"), fixture.requestSummary())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_root_isAuthorized() = runTest {
    val fixture = fixture(UserType.Root, responses = listOf(Stub(200, "{\"tags\":[\"root-tag\"]}")))
    try {
      assertEquals(listOf("root-tag"), fixture.repository.loadTags().getOrThrow())
      assertEquals(1, fixture.requests.size)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_server403_isFailure() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(403, "{}")))
    try {
      val result = fixture.repository.loadTags()

      assertTrue(result.isFailure)
      assertEquals(1, fixture.requests.size)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun rename_successReportsOutcomeAndRefreshesCache() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses =
          listOf(
            Stub(200, "{\"numItemsUpdated\":3,\"tagMerged\":true}"),
            Stub(200, "{\"tags\":[\"new\"]}"),
          ),
      )
    try {
      val result = fixture.repository.renameTag("old", "new").getOrThrow()

      assertEquals(TagMutation(updatedItemCount = 3, merged = true), result)
      assertEquals(listOf("new"), fixture.tagRepository.listTags())
      assertEquals(listOf("POST /api/tags/rename", "GET /api/tags"), fixture.requestSummary())
      assertEquals("{\"tag\":\"old\",\"newTag\":\"new\"}", fixture.bodies[0])
    } finally {
      fixture.close()
    }
  }

  @Test
  fun delete_successUsesEncodedValueReportsOutcomeAndRefreshesCache() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses =
          listOf(
            Stub(200, "{\"numItemsUpdated\":4}"),
            Stub(200, "{\"tags\":[]}"),
          ),
      )
    try {
      val result = fixture.repository.deleteTag("a/b + 日本").getOrThrow()

      assertEquals(TagMutation(updatedItemCount = 4), result)
      assertTrue(fixture.requests[0].url.toString().contains("/api/tags/"))
      assertFalse(fixture.requests[0].url.toString().contains("a/b"))
      assertEquals("GET /api/tags", fixture.requestSummary()[1])
      assertTrue(fixture.tagRepository.listTags().isEmpty())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun failedMutationLeavesCanonicalCachedTagsUnchanged() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses =
          listOf(
            Stub(200, "{\"tags\":[\"canonical\"]}"),
            Stub(500, "{}"),
          ),
      )
    try {
      fixture.repository.loadTags().getOrThrow()
      val result = fixture.repository.renameTag("canonical", "changed")

      assertTrue(result.isFailure)
      assertEquals(listOf("canonical"), fixture.tagRepository.listTags())
      assertEquals(listOf("GET /api/tags", "POST /api/tags/rename"), fixture.requestSummary())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun loadGenres_adminSortsGenres() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses = listOf(Stub(200, "{\"genres\":[\"zeta\",\"Alpha\",\"beta\"]}")),
      )
    try {
      assertEquals(listOf("Alpha", "beta", "zeta"), fixture.repository.loadGenres().getOrThrow())
      assertEquals(listOf("GET /api/genres"), fixture.requestSummary())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun loadGenres_server403_isFailure() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(403, "{}")))
    try {
      assertTrue(fixture.repository.loadGenres().isFailure)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun renameGenre_sendsContractAndReportsMergeOutcome() = runTest {
    val fixture =
      fixture(
        UserType.Root,
        responses = listOf(Stub(200, "{\"numItemsUpdated\":3,\"genreMerged\":true}")),
      )
    try {
      assertEquals(
        GenreMutation(updatedItemCount = 3, merged = true),
        fixture.repository.renameGenre("old", "new").getOrThrow(),
      )
      assertEquals(listOf("POST /api/genres/rename"), fixture.requestSummary())
      assertEquals("{\"genre\":\"old\",\"newGenre\":\"new\"}", fixture.bodies[0])
    } finally {
      fixture.close()
    }
  }

  @Test
  fun deleteGenre_encodesSpacesPunctuationAndUnicode() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(200, "{\"numItemsUpdated\":4}")))
    try {
      assertEquals(
        GenreMutation(updatedItemCount = 4),
        fixture.repository.deleteGenre("Sci-Fi / 日本").getOrThrow(),
      )
      val path = fixture.requests.single().url.encodedPath
      assertTrue(path.startsWith("/api/genres/"))
      assertFalse(path.contains("Sci-Fi"))
      assertFalse(path.contains("日本"))
    } finally {
      fixture.close()
    }
  }

  @Test
  fun blankGenreRename_isRejectedBeforeRequest() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(200, "{}")))
    try {
      val error = fixture.repository.renameGenre("old", "  ").exceptionOrNull()
      assertTrue(error is MetadataValidationException)
      assertEquals(
        MetadataValidationError.GenreNameRequired,
        (error as MetadataValidationException).error,
      )
      assertTrue(fixture.requests.isEmpty())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun failedGenreMutation_propagatesFailureWithoutReloadingGenres() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses =
          listOf(
            Stub(200, "{\"genres\":[\"canonical\"]}"),
            Stub(500, "{}"),
          ),
      )
    try {
      fixture.repository.loadGenres().getOrThrow()
      val result = fixture.repository.renameGenre("canonical", "changed")

      assertTrue(result.isFailure)
      assertEquals(listOf("GET /api/genres", "POST /api/genres/rename"), fixture.requestSummary())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun loadCustomProviders_adminLoadsBookProvidersWithoutPersistingSecrets() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responses =
          listOf(
            Stub(
              200,
              """{"providers":[{"id":"provider-1","name":"Community","url":"https://provider.example","mediaType":"book","slug":"custom-provider-1","authHeaderValue":"Bearer secret"}]}""",
            )
          ),
      )
    try {
      val providers = fixture.repository.loadCustomMetadataProviders().getOrThrow()

      assertEquals("provider-1", providers.single().id)
      assertEquals("Bearer secret", providers.single().authHeaderValue)
      assertEquals(listOf("GET /api/custom-metadata-providers"), fixture.requestSummary())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun createCustomProvider_sendsBookMediaTypeAndOptionalAuthHeader() = runTest {
    val fixture =
      fixture(
        UserType.Root,
        responses =
          listOf(
            Stub(
              200,
              """{"provider":{"id":"provider-1","name":"Community","url":"https://provider.example","mediaType":"book","slug":"custom-provider-1"}}""",
            )
          ),
      )
    try {
      val provider =
        fixture.repository
          .createCustomMetadataProvider(
            " Community ",
            " https://provider.example ",
            "Bearer secret",
          )
          .getOrThrow()

      assertEquals("provider-1", provider.id)
      assertEquals(
        "{\"name\":\"Community\",\"url\":\"https://provider.example\",\"mediaType\":\"book\",\"authHeaderValue\":\"Bearer secret\"}",
        fixture.bodies.single(),
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun createCustomProvider_rejectsRequiredFieldsBeforeRequest() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(200, "{}")))
    try {
      val nameError =
        fixture.repository
          .createCustomMetadataProvider(" ", "https://provider.example", "secret")
          .exceptionOrNull()
      assertTrue(nameError is MetadataValidationException)
      assertEquals(
        MetadataValidationError.CustomMetadataProviderNameRequired,
        (nameError as MetadataValidationException).error,
      )
      val urlError = fixture.repository.createCustomMetadataProvider("Provider", " ", null)
        .exceptionOrNull()
      assertTrue(urlError is MetadataValidationException)
      assertEquals(
        MetadataValidationError.CustomMetadataProviderUrlRequired,
        (urlError as MetadataValidationException).error,
      )
      assertTrue(fixture.requests.isEmpty())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun createCustomProvider_serverValidationFailureIsPropagated() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(400, "Invalid url")))
    try {
      val result = fixture.repository.createCustomMetadataProvider("Provider", "not-a-url", null)
      assertTrue(result.isFailure)
      assertEquals("Invalid url", result.exceptionOrNull()?.message)
      assertEquals(listOf("POST /api/custom-metadata-providers"), fixture.requestSummary())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun deleteCustomProvider_sendsIdAndReportsSuccessWithoutLibraryCount() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(200, "{}")))
    try {
      assertTrue(fixture.repository.deleteCustomMetadataProvider("provider-1").isSuccess)
      assertEquals(
        listOf("DELETE /api/custom-metadata-providers/provider-1"),
        fixture.requestSummary(),
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun customProviderOperations_server403_areFailures() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(403, "{}")))
    try {
      assertTrue(fixture.repository.loadCustomMetadataProviders().isFailure)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun createCustomProvider_server403_isFailure() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(403, "{}")))
    try {
      assertTrue(
        fixture.repository
          .createCustomMetadataProvider("Provider", "https://provider.example", null)
          .isFailure
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun deleteCustomProvider_server403_isFailure() = runTest {
    val fixture = fixture(UserType.Admin, responses = listOf(Stub(403, "{}")))
    try {
      assertTrue(fixture.repository.deleteCustomMetadataProvider("provider-1").isFailure)
    } finally {
      fixture.close()
    }
  }

  private fun fixture(type: UserType, responses: List<Stub>): Fixture {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = Files.createTempFile("metadata-utilities", ".preferences_pb").toFile()
    file.deleteOnExit()
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    val dataStoreManager = DataStoreManager(dataStore)
    runBlocking {
      dataStoreManager.updateUserPrefs(
        UserPrefs(type = type, isAdmin = type == UserType.Admin || type == UserType.Root)
      )
    }

    val requests = mutableListOf<Request>()
    val bodies = mutableListOf<String?>()
    val client =
      OkHttpClient.Builder()
        .cookieJar(SessionCookieJar())
        .addInterceptor(HostSelectionInterceptor(dataStoreManager))
        .addInterceptor { chain ->
          requests += chain.request()
          bodies +=
            chain.request().body?.let { body -> Buffer().also { body.writeTo(it) }.readUtf8() }
          val stub = responses.getOrElse(requests.lastIndex) { responses.last() }
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(stub.code)
            .message(if (stub.code in 200..299) "OK" else "Error")
            .body(stub.body.toResponseBody("application/json".toMediaType()))
            .build()
        }
        .build()
    val json = Json {
      ignoreUnknownKeys = true
      explicitNulls = false
    }
    val api =
      Retrofit.Builder()
        .baseUrl(AudiobookshelfBaseUrl.DEFAULT_VALUE)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(ResultCallAdapterFactory.create())
        .build()
        .create(ApiService::class.java)
    val tagRepository = TagRepository(api, dataStoreManager)
    val helper = Helper(dataStoreManager, ContextWrapper(null))
    return Fixture(
      repository = MetadataUtilsRepository(api, tagRepository, helper),
      tagRepository = tagRepository,
      requests = requests,
      bodies = bodies,
      scope = scope,
      file = file,
    )
  }

  private data class Fixture(
    val repository: MetadataUtilsRepository,
    val tagRepository: TagRepository,
    val requests: MutableList<Request>,
    val bodies: MutableList<String?>,
    val scope: CoroutineScope,
    val file: File,
  ) {
    fun requestSummary(): List<String> = requests.map { "${it.method} ${it.url.encodedPath}" }

    fun close() {
      scope.cancel()
      file.delete()
    }
  }

  private data class Stub(val code: Int, val body: String)
}
