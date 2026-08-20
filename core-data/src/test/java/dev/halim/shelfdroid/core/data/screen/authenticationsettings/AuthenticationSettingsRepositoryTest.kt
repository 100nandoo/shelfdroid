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
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.datastore.DataStoreManager
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
import okio.Buffer
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

  @Test
  fun discover_root_usesAuthenticatedServerRouteAndMergesProviderMetadata() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, issuerConfigurationJson()),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val draft =
        loaded.draftSettings!!.copy(
          openId =
            loaded.draftSettings.openId.copy(
              clientId = "edited-client",
              mobileRedirectUris = listOf("audiobookshelf://oauth", "https://mobile.example/cb"),
              buttonText = "Company login",
              subfolderForRedirectUrls = "/audiobookshelf",
            )
        )
      val discovered =
        fixture.repository.discover(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(),
          )
        )

      assertTrue(discovered.state is AuthenticationSettingsState.Ready)
      val settings = discovered.draftSettings!!.openId
      assertEquals("https://issuer.example.com/", settings.issuerUrl)
      assertEquals("https://issuer.example.com/authorize-new", settings.authorizationUrl)
      assertEquals("https://issuer.example.com/token-new", settings.tokenUrl)
      assertEquals("https://issuer.example.com/userinfo-new", settings.userInfoUrl)
      assertEquals("https://issuer.example.com/jwks-new", settings.jwksUrl)
      assertEquals("https://issuer.example.com/logout-new", settings.logoutUrl)
      assertEquals("edited-client", settings.clientId)
      assertEquals(
        listOf("audiobookshelf://oauth", "https://mobile.example/cb"),
        settings.mobileRedirectUris,
      )
      assertEquals("/audiobookshelf", settings.subfolderForRedirectUrls)
      assertEquals("Company login", settings.buttonText)
      assertEquals("RS256", settings.tokenSigningAlgorithm)
      assertEquals(listOf("RS256", "ES256"), discovered.signingAlgorithmOptions)
      assertTrue(
        discovered.apiState ==
          AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Discovery)
      )
      assertEquals(
        "https://audiobooks.dev/auth/openid/config?issuer=https%3A%2F%2Fissuer.example.com",
        fixture.requestedUrls[1],
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun discover_subpath_preservesConfiguredBasePath() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses = listOf(Stub(200, completeSettingsJson()), Stub(200, issuerConfigurationJson())),
      )
    try {
      fixture.dataStoreManager.updateBaseUrl("https://example.com/audiobookshelf/")
      val loaded = fixture.repository.load()
      fixture.repository.discover(loaded)

      assertEquals(
        "https://example.com/audiobookshelf/auth/openid/config?issuer=https%3A%2F%2Fissuer.example.com",
        fixture.requestedUrls[1],
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun discover_failure_preservesDraftAndReportsDiscoveryOperation() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses = listOf(Stub(200, completeSettingsJson()), Stub(500, "provider unavailable")),
      )
    try {
      val loaded = fixture.repository.load()
      val draft =
        loaded.draftSettings!!.copy(
          openId = loaded.draftSettings.openId.copy(clientId = "draft-client")
        )
      val result =
        fixture.repository.discover(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(),
          )
        )

      assertEquals(draft, result.draftSettings)
      assertTrue(result.apiState is AuthenticationSettingsApiState.Failure)
      assertEquals(
        AuthenticationSettingsOperation.Discovery,
        (result.apiState as AuthenticationSettingsApiState.Failure).operation,
      )
      assertEquals(2, fixture.requestedUrls.size)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun discover_nonAdmin_doesNotSendHttpRequest() = runTest {
    val fixture = fixture(UserType.User, responseBody = completeSettingsJson())
    try {
      val result = fixture.repository.discover(AuthenticationSettingsUiState())

      assertEquals(AuthenticationSettingsState.AccessDenied, result.state)
      assertTrue(fixture.requestedUrls.isEmpty())
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_openIdChangeShowsRestartRequiredAfterCanonicalReload() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, "{\"updated\":true}"),
            Stub(200, completeSettingsJson()),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val draft =
        loaded.draftSettings!!.copy(
          openId = loaded.draftSettings.openId.copy(clientId = "new-client")
        )
      val saved =
        fixture.repository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(),
          )
        )

      assertTrue(saved.restartRequired)
      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)
      assertTrue(fixture.requestBodies[1]!!.contains("authOpenIDClientID"))
      assertFalse(fixture.requestBodies[1]!!.contains("authOpenIDIssuerURL"))
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_changedCallbacksSendsOnlyCallbackFieldsAndReloadsCanonicalSettings() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, "{\"updated\":true}"),
            Stub(200, completeSettingsJson()),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val draft =
        loaded.draftSettings!!.copy(
          openId =
            loaded.draftSettings.openId.copy(
              mobileRedirectUris = listOf("audiobookshelf://oauth", "sampleapp://oauth/callback"),
              subfolderForRedirectUrls = "",
            )
        )
      val saved =
        fixture.repository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation =
              draft.validation(callbackSubfolderOptions = loaded.callbackSubfolderOptions),
          )
        )

      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)
      assertTrue(saved.restartRequired)
      assertEquals(
        "{\"authOpenIDMobileRedirectURIs\":[\"audiobookshelf://oauth\",\"sampleapp://oauth/callback\"],\"authOpenIDSubfolderForRedirectURLs\":\"\"}",
        fixture.requestBodies[1],
      )
      assertEquals(3, fixture.requestedUrls.size)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_mappingChangeSendsOnlyMappingFieldsAndReloadsCanonicalSettings() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, "{\"updated\":true}"),
            Stub(200, completeSettingsJson()),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val draft =
        loaded.draftSettings!!.copy(
          openId =
            loaded.draftSettings.openId.copy(
              buttonText = "Continue with Acme",
              matchExistingBy = "username",
              autoLaunch = false,
              autoRegister = true,
              groupClaim = "roles",
              advancedPermsClaim = "abspermissions",
            )
        )
      val saved =
        fixture.repository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation =
              draft.validation(callbackSubfolderOptions = loaded.callbackSubfolderOptions),
          )
        )

      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)
      assertTrue(saved.restartRequired)
      assertEquals(
        "{\"authOpenIDButtonText\":\"Continue with Acme\",\"authOpenIDMatchExistingBy\":\"username\",\"authOpenIDAutoLaunch\":false,\"authOpenIDAutoRegister\":true,\"authOpenIDGroupClaim\":\"roles\",\"authOpenIDAdvancedPermsClaim\":\"abspermissions\"}",
        fixture.requestBodies[1],
      )
      assertEquals(3, fixture.requestedUrls.size)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_noChangesDoesNotSendPatch() = runTest {
    val fixture = fixture(UserType.Admin, responseBody = completeSettingsJson())
    try {
      val loaded = fixture.repository.load()
      val saved = fixture.repository.save(loaded)

      assertEquals(loaded.savedSettings, saved.savedSettings)
      assertEquals(1, fixture.requestedUrls.size)
      assertTrue(saved.apiState is AuthenticationSettingsApiState.Idle)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_partialPatchReloadsCanonicalSettings() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, "{\"updated\":true}"),
            Stub(
              200,
              completeSettingsJson()
                .replace("<p>Welcome</p>", "<p>Canonical</p>")
                .replace("[\"local\", \"openid\"]", "[\"openid\"]"),
            ),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val draft =
        loaded.draftSettings!!.copy(
          customMessage = "<p>Changed</p>",
          customMessageEnabled = true,
        )
      val saved =
        fixture.repository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(),
          )
        )

      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)
      assertEquals("<p>Canonical</p>", saved.draftSettings?.customMessage)
      assertEquals(3, fixture.requestedUrls.size)
      assertEquals(
        "{\"authLoginCustomMessage\":\"<p>Changed</p>\"}",
        fixture.requestBodies[1],
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_secretReplacementSendsOnlyReplacementAndReloadsConfiguredState() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, "{\"updated\":true}"),
            Stub(200, completeSettingsJson()),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val saved =
        fixture.repository.save(
          loaded.copy(clientSecretChangePending = true),
          AuthenticationSettingsSecretUpdate.Replace("replacement-secret"),
        )

      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)
      assertTrue(saved.draftSettings!!.openId.clientSecretConfigured)
      assertEquals(
        "{\"authOpenIDClientSecret\":\"replacement-secret\"}",
        fixture.requestBodies[1],
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_clearSecretSendsExplicitEmptyString() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses =
          listOf(
            Stub(200, completeSettingsJson()),
            Stub(200, "{\"updated\":true}"),
            Stub(200, completeSettingsJson()),
          ),
      )
    try {
      val loaded = fixture.repository.load()
      val draft = loaded.draftSettings!!.copy(activeLoginMethods = listOf(LoginMethod.Local))
      val saved =
        fixture.repository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(AuthenticationSettingsSecretUpdate.Clear),
            clientSecretChangePending = true,
          ),
          AuthenticationSettingsSecretUpdate.Clear,
        )

      assertTrue(saved.apiState is AuthenticationSettingsApiState.Success)
      assertEquals(
        "{\"authActiveAuthMethods\":[\"local\"],\"authOpenIDClientSecret\":\"\"}",
        fixture.requestBodies[1],
      )
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_clearConfiguredSecretIsBlockedWhenOpenIdRemainsEnabled() = runTest {
    val fixture = fixture(UserType.Admin, responseBody = completeSettingsJson())
    try {
      val loaded = fixture.repository.load()
      val blocked =
        fixture.repository.save(
          loaded.copy(clientSecretChangePending = true),
          AuthenticationSettingsSecretUpdate.Clear,
        )

      assertTrue(
        AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete in
          blocked.validation.errors
      )
      assertEquals(1, fixture.requestedUrls.size)
    } finally {
      fixture.close()
    }
  }

  @Test
  fun save_updatedFalseSurfacesRejectedWithoutReload() = runTest {
    val fixture =
      fixture(
        UserType.Admin,
        responseBody = completeSettingsJson(),
        responses = listOf(Stub(200, completeSettingsJson()), Stub(200, "{\"updated\":false}")),
      )
    try {
      val loaded = fixture.repository.load()
      val draft = loaded.draftSettings!!.copy(customMessage = "<p>Changed</p>")
      val saved =
        fixture.repository.save(
          loaded.copy(
            state = AuthenticationSettingsState.Ready(draft),
            draftSettings = draft,
            validation = draft.validation(),
          )
        )

      assertEquals(AuthenticationSettingsApiState.Rejected, saved.apiState)
      assertEquals(2, fixture.requestedUrls.size)
    } finally {
      fixture.close()
    }
  }

  private fun fixture(
    type: UserType,
    responseCode: Int = 200,
    responseBody: String,
    responses: List<Stub> = listOf(Stub(responseCode, responseBody)),
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
    val requestBodies = mutableListOf<String?>()
    val client =
      OkHttpClient.Builder()
        .cookieJar(SessionCookieJar())
        .addInterceptor(HostSelectionInterceptor(dataStoreManager))
        .addInterceptor { chain ->
          val requestIndex = requestedUrls.size
          requestedUrls += chain.request().url.toString()
          requestBodies +=
            chain.request().body?.let { body ->
              Buffer().also { buffer -> body.writeTo(buffer) }.readUtf8()
            }
          val stub = responses.getOrElse(requestIndex) { responses.last() }
          response(chain.request(), stub.code, stub.body)
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
    val repository =
      AuthenticationSettingsRepository(
        api = api,
        adminDestinationGuard = AdminDestinationGuard(PrefsRepository(dataStoreManager)),
      )
    return Fixture(repository, dataStoreManager, requestedUrls, requestBodies, scope, file)
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
    """
      .trimIndent()

  private fun issuerConfigurationJson(): String =
    """
    {
      "issuer": "https://issuer.example.com/",
      "authorization_endpoint": "https://issuer.example.com/authorize-new",
      "token_endpoint": "https://issuer.example.com/token-new",
      "userinfo_endpoint": "https://issuer.example.com/userinfo-new",
      "end_session_endpoint": "https://issuer.example.com/logout-new",
      "jwks_uri": "https://issuer.example.com/jwks-new",
      "id_token_signing_alg_values_supported": ["RS256", "ES256"]
    }
    """
      .trimIndent()

  private data class Fixture(
    val repository: AuthenticationSettingsRepository,
    val dataStoreManager: DataStoreManager,
    val requestedUrls: MutableList<String>,
    val requestBodies: MutableList<String?>,
    val scope: CoroutineScope,
    val file: File,
  ) {
    fun close() {
      scope.cancel()
      file.delete()
    }
  }

  private data class Stub(val code: Int, val body: String)
}
