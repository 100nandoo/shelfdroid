package dev.halim.shelfdroid.core.data.screen.login

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sun.net.httpserver.HttpServer
import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.ApiService
import dev.halim.core.network.client.AnonymousRequestTag
import dev.halim.core.network.client.SessionCookieJar
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
            loginDiscoveryMessage = LoginDiscoveryMessage.LocalLoginUnavailable,
            authOpenIdButtonText = "Login with SSO",
          )
        )

      assertEquals(0, loginRequests)
      assertEquals(
        GenericState.Idle,
        result.loginState,
      )
      assertEquals(LoginDiscoveryMessage.LocalLoginUnavailable, result.loginDiscoveryMessage)
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
            loginDiscoveryMessage = LoginDiscoveryMessage.MethodsUnconfirmed,
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

  @Test
  fun startOpenIdLogin_whenServerHasSubpath_buildsMobileAuthorizationUrlAndPersistsPendingContext() =
    runTest {
      val dataStoreScope = dataStoreScope()
      var startRequest: Request? = null
      try {
        val dataStoreManager = dataStoreManager(dataStoreScope)
        val pendingStore = PendingOpenIdLoginStore(dataStoreManager)
        val repository =
          repository(dataStoreManager) { request ->
            startRequest = request
            redirectResponse(
              request = request,
              location = "https://login.example.com/authorize?client_id=abs-mobile",
            )
          }

        val result =
          repository.startOpenIdLogin(
            uiState =
              LoginUiState(
                server = "https://Example.com/audiobookshelf/",
                normalizedServer = "https://example.com/audiobookshelf",
                discoveryState = LoginDiscoveryState.Success,
                availableLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
                authOpenIdButtonText = "Continue with Acme SSO",
              ),
            redirectUri = "dev.halim.shelfdroid.debug://oauth",
          )

        assertEquals("https://example.com/audiobookshelf", result.uiState.server)
        assertEquals("https://example.com/audiobookshelf", result.uiState.normalizedServer)
        assertEquals(
          "https://login.example.com/authorize?client_id=abs-mobile",
          result.authorizationUrl,
        )
        val request = requireNotNull(startRequest)
        val parsed = URI(request.url.toString())
        assertEquals("https", parsed.scheme)
        assertEquals("example.com", parsed.host)
        assertEquals("/audiobookshelf/auth/openid", parsed.path)

        val query = parseQuery(parsed.rawQuery)
        assertEquals("dev.halim.shelfdroid.debug://oauth", query["redirect_uri"])
        assertEquals("code", query["response_type"])
        assertEquals("S256", query["code_challenge_method"])
        assertNotNull(query["state"])
        assertNotNull(query["code_challenge"])
        assertNull(request.header("Authorization"))
        assertSame(AnonymousRequestTag, request.tag(AnonymousRequestTag::class.java))

        val pending = pendingStore.current()
        assertNotNull(pending)
        requireNotNull(pending)
        assertEquals("https://example.com/audiobookshelf", pending.normalizedServer)
        assertEquals(query["state"], pending.state)
        assertTrue(pending.codeVerifier.isNotBlank())
        assertTrue(pending.createdAtEpochMillis > 0)
        assertEquals(expectedCodeChallenge(pending.codeVerifier), query["code_challenge"])
      } finally {
        dataStoreScope.cancel()
      }
    }

  @Test
  fun startOpenIdLogin_whenServerIsInvalid_setsFieldErrorAndDoesNotPersistPendingContext() =
    runTest {
      val dataStoreScope = dataStoreScope()
      try {
        val dataStoreManager = dataStoreManager(dataStoreScope)
        val pendingStore = PendingOpenIdLoginStore(dataStoreManager)
        val repository = repository(dataStoreManager)

        val result =
          repository.startOpenIdLogin(
            uiState = LoginUiState(server = "ftp://example.com"),
            redirectUri = "dev.halim.shelfdroid://oauth",
          )

        assertEquals(LoginFieldError.InvalidServerUrl, result.uiState.serverFieldError)
        assertNull(result.authorizationUrl)
        assertNull(pendingStore.current())
      } finally {
        dataStoreScope.cancel()
      }
    }

  @Test
  fun startOpenIdLogin_whenBootstrapFails_showsFailureAndDoesNotPersistPendingContext() = runTest {
    val dataStoreScope = dataStoreScope()
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      val pendingStore = PendingOpenIdLoginStore(dataStoreManager)
      val repository =
        repository(dataStoreManager) { request ->
          jsonResponse(request = request, code = 400, body = """{"error":"Invalid redirect_uri"}""")
        }

      val result =
        repository.startOpenIdLogin(
          uiState =
            LoginUiState(
              server = "https://example.com",
              normalizedServer = "https://example.com",
              discoveryState = LoginDiscoveryState.Success,
              availableLoginMethods = listOf(LoginMethod.OpenId),
            ),
          redirectUri = "dev.halim.shelfdroid://oauth",
        )

      assertNull(result.authorizationUrl)
      assertTrue(result.uiState.loginState is GenericState.Failure)
      assertNull(pendingStore.current())
    } finally {
      dataStoreScope.cancel()
    }
  }

  @Test
  fun completeOpenIdLogin_reusesBootstrapCookiesAndRunsSharedSuccessPath() = runTest {
    val dataStoreScope = dataStoreScope()
    var startRequest: Request? = null
    var callbackRequest: Request? = null
    var callbackCookieHeader: String? = null
    var server: HttpServer? = null
    try {
      val dataStoreManager = dataStoreManager(dataStoreScope)
      val pendingStore = PendingOpenIdLoginStore(dataStoreManager)
      val callbackStore = PendingOpenIdCallbackStore(dataStoreManager)
      val successHandler = RecordingLoginSuccessHandler()
      server =
        HttpServer.create(InetSocketAddress(0), 0).apply {
          createContext("/audiobookshelf/auth/openid") { exchange ->
            exchange.responseHeaders.add(
              "Location",
              "https://login.example.com/authorize?client_id=abs-mobile",
            )
            exchange.responseHeaders.add("Set-Cookie", "connect.sid=session123; Path=/; HttpOnly")
            exchange.responseHeaders.add(
              "Set-Cookie",
              "auth_method=openid-mobile; Path=/; HttpOnly",
            )
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
          }
          createContext("/audiobookshelf/auth/openid/callback") { exchange ->
            callbackCookieHeader = exchange.requestHeaders.getFirst("Cookie")
            val body =
              """
              {
                "user": {
                  "id": "user-1",
                  "username": "fernando",
                  "type": "admin",
                  "token": "legacy-access-token",
                  "refreshToken": "refresh-token",
                  "permissions": {
                    "download": true,
                    "update": true,
                    "delete": true,
                    "upload": true
                  }
                },
                "serverSettings": {
                  "version": "2.31.0",
                  "logLevel": 1
                }
              }
              """
                .trimIndent()
                .toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
          }
          start()
        }
      val repository =
        repository(
          dataStoreManager = dataStoreManager,
          okHttpClient =
            OkHttpClient.Builder()
              .cookieJar(SessionCookieJar())
              .addInterceptor { chain ->
                val request = chain.request()
                when (request.url.encodedPath) {
                  "/audiobookshelf/auth/openid" -> startRequest = request
                  "/audiobookshelf/auth/openid/callback" -> callbackRequest = request
                }
                chain.proceed(request)
              }
              .build(),
          loginSuccessHandler = successHandler,
        )

      val startResult =
        repository.startOpenIdLogin(
          uiState =
            LoginUiState(
              server = "http://127.0.0.1:${server.address.port}/audiobookshelf",
              normalizedServer = "http://127.0.0.1:${server.address.port}/audiobookshelf",
              discoveryState = LoginDiscoveryState.Success,
              availableLoginMethods = listOf(LoginMethod.OpenId),
            ),
          redirectUri = "dev.halim.shelfdroid.debug://oauth",
        )

      val pending = requireNotNull(pendingStore.current())
      callbackStore.save(
        PendingOpenIdCallback(
          normalizedServer = pending.normalizedServer,
          state = pending.state,
          code = "callback-code",
          receivedAtEpochMillis = pending.createdAtEpochMillis + 1L,
        )
      )

      val result = repository.completeOpenIdLogin()

      assertEquals(OpenIdLoginCompletionResult.Success, result)
      assertEquals(
        "https://login.example.com/authorize?client_id=abs-mobile",
        startResult.authorizationUrl,
      )
      val recordedStartRequest = requireNotNull(startRequest)
      val startQuery = parseQuery(recordedStartRequest.url.encodedQuery)
      assertEquals("dev.halim.shelfdroid.debug://oauth", startQuery["redirect_uri"])
      assertEquals("code", startQuery["response_type"])
      assertEquals("S256", startQuery["code_challenge_method"])
      assertNull(recordedStartRequest.header("Authorization"))
      assertSame(
        AnonymousRequestTag,
        recordedStartRequest.tag(AnonymousRequestTag::class.java),
      )
      val request = requireNotNull(callbackRequest)
      val query = parseQuery(request.url.encodedQuery)
      assertEquals(pending.state, query["state"])
      assertEquals("callback-code", query["code"])
      assertEquals(pending.codeVerifier, query["code_verifier"])
      assertNull(request.header("Authorization"))
      assertSame(AnonymousRequestTag, request.tag(AnonymousRequestTag::class.java))
      val cookieHeader = requireNotNull(callbackCookieHeader)
      assertTrue(cookieHeader.contains("connect.sid=session123"))
      assertTrue(cookieHeader.contains("auth_method=openid-mobile"))
      assertNull(pendingStore.current())
      assertNull(callbackStore.current())
      assertNull(OpenIdLoginFailureStore(dataStoreManager).consume())
      assertEquals("http://127.0.0.1:${server.address.port}/audiobookshelf", successHandler.server)
      val successResponse = requireNotNull(successHandler.response)
      assertEquals("legacy-access-token", successResponse.user.accessToken)
      assertEquals("refresh-token", successResponse.user.refreshToken)
    } finally {
      server?.stop(0)
      dataStoreScope.cancel()
    }
  }

  private fun repository(
    dataStoreManager: DataStoreManager,
    okHttpClient: OkHttpClient,
    loginSuccessHandler: LoginSuccessHandler = NoOpLoginSuccessHandler,
  ): LoginRepository {
    return LoginRepository(
      api = apiService(okHttpClient),
      okHttpClient = okHttpClient,
      dataStoreManager = dataStoreManager,
      prefsRepository = PrefsRepository(dataStoreManager),
      loginSuccessHandler = loginSuccessHandler,
      pendingOpenIdLoginStore = PendingOpenIdLoginStore(dataStoreManager),
      pendingOpenIdCallbackStore = PendingOpenIdCallbackStore(dataStoreManager),
      openIdLoginFailureStore = OpenIdLoginFailureStore(dataStoreManager),
    )
  }

  private fun repository(
    dataStoreManager: DataStoreManager,
    loginSuccessHandler: LoginSuccessHandler = NoOpLoginSuccessHandler,
    respond: (Request) -> Response = { request -> jsonResponse(request, body = """{}""") },
  ): LoginRepository {
    return repository(
      dataStoreManager = dataStoreManager,
      okHttpClient = okHttpClient(respond),
      loginSuccessHandler = loginSuccessHandler,
    )
  }

  private fun repository(
    scope: CoroutineScope,
    respond: (Request) -> Response,
  ): LoginRepository = repository(dataStoreManager = dataStoreManager(scope), respond = respond)

  private fun apiService(okHttpClient: OkHttpClient): ApiService {
    val json = Json {
      coerceInputValues = true
      ignoreUnknownKeys = true
      isLenient = true
      prettyPrint = true
      explicitNulls = false
    }

    return Retrofit.Builder()
      .baseUrl(AudiobookshelfBaseUrl.DEFAULT_VALUE)
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .addCallAdapterFactory(ResultCallAdapterFactory.create())
      .build()
      .create(ApiService::class.java)
  }

  private fun okHttpClient(respond: (Request) -> Response): OkHttpClient {
    return OkHttpClient.Builder()
      .cookieJar(SessionCookieJar())
      .addInterceptor { chain -> respond(chain.request()) }
      .build()
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

  private fun redirectResponse(
    request: Request,
    location: String,
    headers: List<Pair<String, String>> = emptyList(),
  ): Response {
    val builder =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(302)
        .message("Found")
        .header("Location", location)
        .body("".toResponseBody("text/plain".toMediaType()))
    headers.forEach { (name, value) -> builder.addHeader(name, value) }
    return builder.build()
  }

  private fun parseQuery(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrBlank()) return emptyMap()
    return rawQuery.split("&").associate { entry ->
      val (rawKey, rawValue) = entry.split("=", limit = 2).let { parts ->
        parts.first() to parts.getOrElse(1) { "" }
      }
      URLDecoder.decode(rawKey, StandardCharsets.UTF_8) to
        URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
    }
  }

  private fun expectedCodeChallenge(codeVerifier: String): String {
    val hash = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
  }

  private data object NoOpLoginSuccessHandler : LoginSuccessHandler {
    override suspend fun onLoginSuccess(
      server: String,
      response: dev.halim.core.network.response.login.LoginResponse,
    ) = Unit
  }

  private class RecordingLoginSuccessHandler : LoginSuccessHandler {
    var server: String? = null
    var response: dev.halim.core.network.response.login.LoginResponse? = null

    override suspend fun onLoginSuccess(
      server: String,
      response: dev.halim.core.network.response.login.LoginResponse,
    ) {
      this.server = server
      this.response = response
    }
  }
}
