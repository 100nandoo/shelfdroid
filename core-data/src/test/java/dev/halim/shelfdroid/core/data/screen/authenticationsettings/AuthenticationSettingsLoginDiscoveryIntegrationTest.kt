package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.ApiService
import dev.halim.core.network.response.login.LoginResponse
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.data.screen.login.LoginRepository
import dev.halim.shelfdroid.core.data.screen.login.LoginSuccessHandler
import dev.halim.shelfdroid.core.data.screen.login.OpenIdLoginFailureStore
import dev.halim.shelfdroid.core.data.screen.login.PendingOpenIdCallbackStore
import dev.halim.shelfdroid.core.data.screen.login.PendingOpenIdLoginStore
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthenticationSettingsLoginDiscoveryIntegrationTest {

  @Test
  fun acceptedAuthenticationPatch_isObservedByLoginDiscovery() = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = Files.createTempFile("authentication-login-discovery", ".preferences_pb").toFile()
    file.deleteOnExit()
    try {
      val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
      val dataStoreManager = DataStoreManager(dataStore)
      dataStoreManager.updateUserPrefs(UserPrefs(type = UserType.Admin, isAdmin = true))

      var customMessage = "<p>Before the update</p>"
      var activeMethods = listOf("local")
      var buttonText = "Before OpenID"
      var autoLaunch = false
      val client =
        OkHttpClient.Builder()
          .addInterceptor { chain ->
            val request = chain.request()
            val path = request.url.encodedPath
            if (request.method == "PATCH" && path == "/api/auth-settings") {
              val body =
                request.body?.let { Buffer().also { buffer -> it.writeTo(buffer) }.readUtf8() }
              if (body?.contains("authLoginCustomMessage") == true) {
                customMessage = "<p>After the update</p>"
              }
              if (body?.contains("authActiveAuthMethods") == true) {
                activeMethods = listOf("local", "openid")
              }
              if (body?.contains("authOpenIDButtonText") == true) {
                buttonText = "Continue with Acme"
              }
              if (body?.contains("authOpenIDAutoLaunch") == true) {
                autoLaunch = true
              }
              response(request, "{\"updated\":true}")
            } else if (path == "/api/auth-settings") {
              response(
                request,
                authenticationSettingsJson(customMessage, activeMethods, buttonText, autoLaunch),
              )
            } else if (path == "/status") {
              response(
                request,
                """
                {
                  "authMethods": ${activeMethods.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\"")},
                  "authFormData": {
                    "authLoginCustomMessage": "$customMessage",
                    "authOpenIDButtonText": "$buttonText",
                    "authOpenIDAutoLaunch": $autoLaunch
                  }
                }
                """
                  .trimIndent(),
              )
            } else {
              response(request, "{}")
            }
          }
          .build()
      val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        isLenient = true
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
      val prefsRepository = PrefsRepository(dataStoreManager)
      val authenticationRepository = AuthenticationSettingsRepository(api = api)
      val loginRepository =
        LoginRepository(
          api = api,
          okHttpClient = client,
          dataStoreManager = dataStoreManager,
          prefsRepository = prefsRepository,
          loginSuccessHandler = NoOpLoginSuccessHandler,
          pendingOpenIdLoginStore = PendingOpenIdLoginStore(dataStoreManager),
          pendingOpenIdCallbackStore = PendingOpenIdCallbackStore(dataStoreManager),
          openIdLoginFailureStore = OpenIdLoginFailureStore(dataStoreManager),
        )

      val loaded = authenticationRepository.load()
      val draft =
        loaded.draftSettings!!.copy(
          customMessage = "<p>After the update</p>",
          customMessageEnabled = true,
          activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
          openId =
            loaded.draftSettings.openId.copy(
              buttonText = "Continue with Acme",
              autoLaunch = true,
            ),
        )
      val saved =
        authenticationRepository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(),
          )
        )
      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)

      val discovered = loginRepository.discoverLoginMethods("https://audiobooks.example")

      assertEquals(LoginDiscoveryState.Success, discovered.discoveryState)
      assertEquals(listOf(LoginMethod.Local, LoginMethod.OpenId), discovered.availableLoginMethods)
      assertEquals("<p>After the update</p>", discovered.authLoginCustomMessage)
      assertEquals("Continue with Acme", discovered.authOpenIdButtonText)
      assertEquals(true, discovered.authOpenIdAutoLaunch)
    } finally {
      scope.cancel()
      file.delete()
    }
  }

  private fun authenticationSettingsJson(
    message: String,
    methods: List<String>,
    buttonText: String,
    autoLaunch: Boolean,
  ): String =
    """
    {
      "authLoginCustomMessage": "$message",
      "authActiveAuthMethods": ["${methods.joinToString("\",\"")}"],
      "authOpenIDIssuerURL": "https://issuer.example.com",
      "authOpenIDAuthorizationURL": "https://issuer.example.com/authorize",
      "authOpenIDTokenURL": "https://issuer.example.com/token",
      "authOpenIDUserInfoURL": "https://issuer.example.com/userinfo",
      "authOpenIDJwksURL": "https://issuer.example.com/jwks",
      "authOpenIDClientID": "shelfdroid",
      "authOpenIDClientSecret": "secret-value",
      "authOpenIDTokenSigningAlgorithm": "RS256",
      "authOpenIDButtonText": "$buttonText",
      "authOpenIDAutoLaunch": $autoLaunch
    }
    """
      .trimIndent()

  private fun response(request: Request, body: String): Response =
    Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(200)
      .message("OK")
      .body(body.toResponseBody("application/json".toMediaType()))
      .build()

  private data object NoOpLoginSuccessHandler : LoginSuccessHandler {
    override suspend fun onLoginSuccess(
      server: String,
      serverAccessMode: ServerAccessMode,
      response: LoginResponse,
    ) = Unit
  }
}
