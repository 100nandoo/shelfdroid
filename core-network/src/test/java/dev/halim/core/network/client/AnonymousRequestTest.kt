package dev.halim.core.network.client

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.Lazy
import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AnonymousRequestTest {

  @Test
  fun intercept_whenRequestIsTaggedAnonymous_skipsAuthorization() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      dataStoreManager.updateUserPrefs(UserPrefs(accessToken = "access-token"))

      var capturedRequest: Request? = null
      val client =
        OkHttpClient.Builder()
          .addInterceptor(HostSelectionInterceptor(dataStoreManager))
          .addInterceptor { chain ->
            capturedRequest = chain.request()
            okResponse(chain.request())
          }
          .build()

      client
        .newCall(
          Request.Builder()
            .url("https://example.com/audiobookshelf/status")
            .tag(AnonymousRequestTag::class.java, AnonymousRequestTag)
            .build()
        )
        .execute()
        .close()
      client.dispatcher.executorService.shutdown()
      client.connectionPool.evictAll()

      val request = requireNotNull(capturedRequest)
      assertNull(request.header("Authorization"))
      assertSame(AnonymousRequestTag, request.tag(AnonymousRequestTag::class.java))
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun authenticate_whenRequestIsAnonymous_doesNotTriggerForcedRelogin() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      dataStoreManager.updateUserPrefs(
        UserPrefs(accessToken = "access-token", refreshToken = "refresh-token")
      )
      val apiService =
        object : Lazy<ApiService> {
          override fun get(): ApiService {
            error("Anonymous requests must not try to refresh tokens.")
          }
        }
      val authenticator = TokenAuthenticator(apiService, dataStoreManager)
      val request =
        Request.Builder()
          .url("https://example.com/audiobookshelf/status")
          .tag(AnonymousRequestTag::class.java, AnonymousRequestTag)
          .build()

      val retryRequest =
        authenticator.authenticate(route = null, response = failedResponse(request))

      assertNull(retryRequest)
      assertEquals("access-token", dataStoreManager.userPrefs.first().accessToken)
      assertEquals("refresh-token", dataStoreManager.userPrefs.first().refreshToken)
      assertEquals(null, dataStoreManager.authPromptReason.first())
    } finally {
      dataStoreScope.cancel()
    }
  }

  private fun dataStoreManager(scope: CoroutineScope): DataStoreManager {
    val file =
      Files.createTempFile("anonymous-request", ".preferences_pb").toFile().apply { deleteOnExit() }
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    return DataStoreManager(dataStore)
  }

  private fun dataStoreScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  private fun okResponse(request: Request): Response {
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body("{}".toResponseBody("application/json".toMediaType()))
      .build()
  }

  private fun failedResponse(request: Request): Response {
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(401)
      .message("Unauthorized")
      .body("{}".toResponseBody("application/json".toMediaType()))
      .build()
  }
}
