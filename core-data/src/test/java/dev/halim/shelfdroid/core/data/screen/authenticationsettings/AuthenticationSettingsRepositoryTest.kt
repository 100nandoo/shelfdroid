package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dev.halim.core.network.ApiService
import dev.halim.core.network.client.HostSelectionInterceptor
import dev.halim.core.network.client.SessionCookieJar
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.admin.AdminDestinationGuard
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import java.io.File
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthenticationSettingsRepositoryTest {

  @Test
  fun load_admin_mapsSettingsAndReducesClientSecret() = runTest {
    val fixture = fixture(UserType.Admin, responseBody = completeSettingsJson())
    try {
      val state = fixture.repository.load().state

      assertTrue(state is AuthenticationSettingsState.Ready)
      val settings = (state as AuthenticationSettingsState.Ready).settings
      assertTrue(settings.customMessageEnabled)
      assertEquals(
        listOf(LoginMethod.Local, LoginMethod.OpenId),
        settings.activeLoginMethods,
      )
      assertEquals("https://issuer.example.com", settings.openId.issuerUrl)
      assertTrue(settings.openId.clientSecretConfigured)
      assertFalse(settings.toString().contains("secret-value"))
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_root_isAllowedByTheSameAdminGuard() = runTest {
    val fixture = fixture(UserType.Root, responseBody = completeSettingsJson())
    try {
      assertTrue(fixture.repository.load().state is AuthenticationSettingsState.Ready)
      assertEquals(1, fixture.requestedUrls.size)
      assertEquals("https://audiobooks.dev/api/auth-settings", fixture.requestedUrls.single())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_nonAdmin_returnsAccessDeniedWithoutAnHttpRequest() = runTest {
    val fixture = fixture(UserType.User, responseBody = completeSettingsJson())
    try {
      assertEquals(
        AuthenticationSettingsState.AccessDenied,
        fixture.repository.load().state,
      )
      assertTrue(fixture.requestedUrls.isEmpty())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_serverForbidden_returnsAccessDeniedWithoutPartialSettings() = runTest {
    val fixture = fixture(UserType.Admin, responseCode = 403, responseBody = "{}")
    try {
      assertEquals(
        AuthenticationSettingsState.AccessDenied,
        fixture.repository.load().state,
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_ordinaryFailure_returnsRetryableFailure() = runTest {
    val fixture = fixture(UserType.Admin, responseCode = 500, responseBody = "{}")
    try {
      val state = fixture.repository.load().state

      assertTrue(state is AuthenticationSettingsState.Failure)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun load_subpathInstallation_preservesBasePath() = runTest {
    val fixture = fixture(UserType.Admin, responseBody = "{}")
    try {
      fixture.dataStoreManager.updateBaseUrl("https://example.com/audiobookshelf/")

      assertTrue(fixture.repository.load().state is AuthenticationSettingsState.Ready)
      assertEquals(
        "https://example.com/audiobookshelf/api/auth-settings",
        fixture.requestedUrls.single(),
      )
    } finally {
      fixture.close()
    }
  }

  private fun fixture(
    type: UserType,
    responseCode: Int = 200,
    responseBody: String,
  ): Fixture {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val file = Files.createTempFile("authentication-settings", ".preferences_pb").toFile()
    file.deleteOnExit()
    val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    val dataStoreManager = DataStoreManager(dataStore)
    kotlinx.coroutines.runBlocking {
      dataStoreManager.updateUserPrefs(
        UserPrefs(type = type, isAdmin = type == UserType.Admin || type == UserType.Root)
      )
    }
    val requestedUrls = mutableListOf<String>()
    val client =
      OkHttpClient.Builder()
        .cookieJar(SessionCookieJar())
        .addInterceptor(HostSelectionInterceptor(dataStoreManager))
        .addInterceptor { chain ->
          requestedUrls += chain.request().url.toString()
          response(chain.request(), responseCode, responseBody)
        }
        .build()
    val json =
      Json {
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
    val repository =
      AuthenticationSettingsRepository(
        api = api,
        adminDestinationGuard = AdminDestinationGuard(PrefsRepository(dataStoreManager)),
      )
    return Fixture(repository, dataStoreManager, requestedUrls, scope, file)
  }

  private fun response(request: Request, code: Int, body: String): Response =
    Response.Builder()
      .request(request)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message(if (code in 200..299) "OK" else "Error")
      .body(body.toResponseBody("application/json".toMediaType()))
      .build()

  private fun completeSettingsJson(): String =
    """
    {
      "authLoginCustomMessage": "<p>Welcome</p>",
      "authActiveAuthMethods": ["local", "openid"],
      "authOpenIDIssuerURL": "https://issuer.example.com",
      "authOpenIDAuthorizationURL": "https://issuer.example.com/authorize",
      "authOpenIDTokenURL": "https://issuer.example.com/token",
      "authOpenIDUserInfoURL": "https://issuer.example.com/userinfo",
      "authOpenIDJwksURL": "https://issuer.example.com/jwks",
      "authOpenIDLogoutURL": "https://issuer.example.com/logout",
      "authOpenIDClientID": "shelfdroid",
      "authOpenIDClientSecret": "secret-value",
      "authOpenIDTokenSigningAlgorithm": "RS256",
      "authOpenIDMobileRedirectURIs": ["audiobookshelf://oauth"],
      "authOpenIDSubfolderForRedirectURLs": "/audiobookshelf",
      "authOpenIDButtonText": "Sign in with the provider",
      "authOpenIDMatchExistingBy": "email",
      "authOpenIDAutoLaunch": true,
      "authOpenIDAutoRegister": false,
      "authOpenIDGroupClaim": "groups",
      "authOpenIDAdvancedPermsClaim": "permissions",
      "authOpenIDSamplePermissions": "{\"download\":true}"
    }
    """.trimIndent()

  private data class Fixture(
    val repository: AuthenticationSettingsRepository,
    val dataStoreManager: DataStoreManager,
    val requestedUrls: MutableList<String>,
    val scope: CoroutineScope,
    val file: File,
  ) {
    fun close() {
      scope.cancel()
      file.delete()
    }
  }
}
