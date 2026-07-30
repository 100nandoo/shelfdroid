package dev.halim.shelfdroid.core.data.screen.login

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

class LoginRepositoryTest {

  @Test
  fun discoverLoginMethods_whenServerHasSubpath_requestsStatusFromNormalizedSubpathUrl() = runTest {
    val dataStoreScope = dataStoreScope()
    var requestedUrl = ""
    try {
      val repository =
        repository(dataStoreScope) { request ->
          requestedUrl = request.url.toString()
          jsonResponse(
            request = request,
            body =
              """
              {
                "authMethods": ["local"],
                "authFormData": {
                  "authLoginCustomMessage": "Use your library account."
                }
              }
              """
                .trimIndent(),
          )
        }

      val result = repository.discoverLoginMethods("https://Example.com/audiobookshelf/")

      assertEquals("https://example.com/audiobookshelf/status", requestedUrl)
      assertEquals("https://example.com/audiobookshelf", result.normalizedServer)
      assertEquals(LoginDiscoveryState.Success, result.discoveryState)
      assertEquals(listOf(LoginMethod.Local), result.availableLoginMethods)
      assertEquals("Use your library account.", result.authLoginCustomMessage)
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun discoverLoginMethods_whenServerIsInvalid_skipsStatusRequestAndClearsDiscovery() = runTest {
    val dataStoreScope = dataStoreScope()
    var requestCount = 0
    try {
      val repository =
        repository(dataStoreScope) { request ->
          requestCount += 1
          jsonResponse(request = request, body = """{"authMethods":["local"]}""")
        }

      val result = repository.discoverLoginMethods("ftp://example.com")

      assertEquals(0, requestCount)
      assertEquals(null, result.normalizedServer)
      assertEquals(LoginDiscoveryState.Idle, result.discoveryState)
      assertEquals(listOf(LoginMethod.Local), result.availableLoginMethods)
      assertEquals(null, result.authLoginCustomMessage)
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun discoverLoginMethods_whenAuthMethodsAreMissing_fallsBackToLocalLogin() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val repository =
        repository(dataStoreScope) { request ->
          jsonResponse(
            request = request,
            body =
              """
              {
                "authFormData": {
                  "authLoginCustomMessage": "Sign in with your usual account."
                }
              }
              """
                .trimIndent(),
          )
        }

      val result = repository.discoverLoginMethods("https://example.com")

      assertEquals(LoginDiscoveryState.Success, result.discoveryState)
      assertEquals(listOf(LoginMethod.Local), result.availableLoginMethods)
      assertEquals("Sign in with your usual account.", result.authLoginCustomMessage)
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun login_whenDiscoveryConfirmedLocalIsUnavailable_blocksCredentialSubmit() = runTest {
    val dataStoreScope = dataStoreScope()
    var loginRequests = 0
    try {
      val repository =
        repository(dataStoreScope) { request ->
          if (request.url.encodedPath.endsWith("/login")) {
            loginRequests += 1
          }
          jsonResponse(request = request, code = 401, body = """{"error":"Unauthorized"}""")
        }

      val result =
        repository.login(
          LoginUiState(
            server = "https://example.com",
            normalizedServer = "https://example.com",
            username = "fernando",
            password = "secret",
            discoveryState = LoginDiscoveryState.Success,
            availableLoginMethods = listOf(LoginMethod.OpenId),
            authOpenIdButtonText = "Login with SSO",
          )
        )

      assertEquals(0, loginRequests)
      assertTrue(result.loginState is GenericState.Failure)
      assertEquals(
        LOCAL_LOGIN_UNAVAILABLE_MESSAGE,
        (result.loginState as GenericState.Failure).errorMessage,
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun login_whenDiscoveryFailed_preservesLocalLoginFallback() = runTest {
    val dataStoreScope = dataStoreScope()
    var loginRequests = 0
    try {
      val repository =
        repository(dataStoreScope) { request ->
          if (request.url.encodedPath.endsWith("/login")) {
            loginRequests += 1
          }
          jsonResponse(request = request, code = 401, body = """{"error":"Unauthorized"}""")
        }

      val result =
        repository.login(
          LoginUiState(
            server = AudiobookshelfBaseUrl.DEFAULT_VALUE,
            normalizedServer = AudiobookshelfBaseUrl.DEFAULT_VALUE.removeSuffix("/"),
            username = "fernando",
            password = "secret",
            discoveryState = LoginDiscoveryState.Failure,
            loginDiscoveryMessage = LOGIN_DISCOVERY_FAILED_MESSAGE,
          )
        )

      assertEquals(1, loginRequests)
      assertTrue(result.loginState is GenericState.Failure)
      assertEquals(
        "Invalid username or password.",
        (result.loginState as GenericState.Failure).errorMessage,
      )
    } finally {
      dataStoreScope.cancel()
    }
  }

  private fun repository(
    scope: CoroutineScope,
    respond: (Request) -> Response,
  ): LoginRepository {
    val dataStoreManager = dataStoreManager(scope)
    return LoginRepository(
      api = apiService(respond),
      dataStoreManager = dataStoreManager,
      prefsRepository = PrefsRepository(dataStoreManager),
      loginSuccessHandler = NoOpLoginSuccessHandler,
    )
  }

  private fun apiService(respond: (Request) -> Response): ApiService {
    val json =
      Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        explicitNulls = false
      }
    val okHttpClient = OkHttpClient.Builder().addInterceptor { chain -> respond(chain.request()) }.build()

    return Retrofit.Builder()
      .baseUrl(AudiobookshelfBaseUrl.DEFAULT_VALUE)
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .addCallAdapterFactory(ResultCallAdapterFactory.create())
      .build()
      .create(ApiService::class.java)
  }

  private fun dataStoreManager(scope: CoroutineScope): DataStoreManager {
    val file =
      Files.createTempFile("login-repository", ".preferences_pb").toFile().apply { deleteOnExit() }
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    return DataStoreManager(dataStore)
  }

  private fun dataStoreScope(): CoroutineScope {
    return CoroutineScope(SupervisorJob() + Dispatchers.IO)
  }

  private fun jsonResponse(request: Request, code: Int = 200, body: String): Response {
    return Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message(if (code in 200..299) "OK" else "Error")
      .body(body.toResponseBody("application/json".toMediaType()))
      .build()
  }

  private data object NoOpLoginSuccessHandler : LoginSuccessHandler {
    override suspend fun onLoginSuccess(
      server: String,
      response: dev.halim.core.network.response.LoginResponse,
    ) = Unit
  }
}
