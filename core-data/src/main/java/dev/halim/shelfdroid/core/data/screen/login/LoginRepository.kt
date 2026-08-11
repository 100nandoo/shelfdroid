package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.ApiService
import dev.halim.core.network.client.AnonymousRequestTag
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.login.LoginResponse
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException

class LoginRepository
@Inject
constructor(
  private val api: ApiService,
  private val okHttpClient: OkHttpClient,
  private val dataStoreManager: DataStoreManager,
  prefsRepository: PrefsRepository,
  private val loginSuccessHandler: LoginSuccessHandler,
  private val pendingOpenIdLoginStore: PendingOpenIdLoginStore,
  private val pendingOpenIdCallbackStore: PendingOpenIdCallbackStore,
  private val openIdLoginFailureStore: OpenIdLoginFailureStore,
) {

  val userPrefs = prefsRepository.userPrefs
  val baseUrl = dataStoreManager.baseUrl()
  private val openIdBootstrapClient by
    lazy(LazyThreadSafetyMode.NONE) {
      okHttpClient.newBuilder().followRedirects(false).build()
    }

  suspend fun currentServerAccessMode(): ServerAccessMode {
    return dataStoreManager.serverPrefs.firstOrNull()?.accessMode ?: ServerAccessMode.Internet
  }

  suspend fun discoverLoginMethods(rawServer: String): LoginDiscoveryResult {
    val parsedServer = AudiobookshelfBaseUrl.parse(rawServer) ?: return LoginDiscoveryResult()
    val response = api.status(parsedServer.resolve("/status"))
    val result = response.getOrNull()
    if (result != null) {
      val availableLoginMethods =
        result.authMethods.toLoginMethods().ifEmpty { listOf(LoginMethod.Local) }
      return LoginDiscoveryResult(
        normalizedServer = parsedServer.value,
        discoveryState = LoginDiscoveryState.Success,
        availableLoginMethods = availableLoginMethods,
        loginDiscoveryMessage =
          if (LoginMethod.Local !in availableLoginMethods) {
            LoginDiscoveryMessage.LocalLoginUnavailable
          } else {
            null
          },
        authLoginCustomMessage =
          result.authFormData?.authLoginCustomMessage?.takeUnless { it.isBlank() },
        authOpenIdButtonText =
          result.authFormData?.authOpenIDButtonText?.takeUnless { it.isBlank() },
        authOpenIdAutoLaunch = result.authFormData?.authOpenIDAutoLaunch,
      )
    }

    return LoginDiscoveryResult(
      normalizedServer = parsedServer.value,
      discoveryState = LoginDiscoveryState.Failure,
      availableLoginMethods = listOf(LoginMethod.Local),
      loginDiscoveryMessage = LoginDiscoveryMessage.MethodsUnconfirmed,
    )
  }

  suspend fun login(uiState: LoginUiState): LoginUiState {
    val normalizedServer =
      uiState.normalizedServer
        ?: AudiobookshelfBaseUrl.parse(uiState.server)?.value
        ?: return uiState.copy(
          loginState = GenericState.Idle,
          serverFieldError = LoginFieldError.InvalidServerUrl,
        )

    if (
      uiState.discoveryState is LoginDiscoveryState.Success &&
        LoginMethod.Local !in uiState.availableLoginMethods
    ) {
      return uiState.copy(
        normalizedServer = normalizedServer,
        loginState = GenericState.Idle,
        serverFieldError = null,
      )
    }

    DataStoreManager.BASE_URL = normalizedServer
    val request = LoginRequest(uiState.username, uiState.password)
    val response = api.login(request)
    val result = response.getOrNull()
    if (result != null) {
      loginSuccessHandler.onLoginSuccess(normalizedServer, uiState.serverAccessMode, result)
      return uiState.copy(
        server = normalizedServer,
        normalizedServer = normalizedServer,
        loginState = GenericState.Success,
        serverFieldError = null,
      )
    }
    val exception = response.exceptionOrNull()
    val message =
      if (exception is HttpException) {
        when (exception.code()) {
          401 -> "Invalid username or password."
          404 -> "Server not found."
          429 -> "Too many requests. Please wait and try again."
          else -> exception.message()
        }
      } else {
        exception?.message
      }
    return uiState.copy(
      server = normalizedServer,
      normalizedServer = normalizedServer,
      loginState = GenericState.Failure(message),
      serverFieldError = null,
    )
  }

  suspend fun startOpenIdLogin(
    uiState: LoginUiState,
    redirectUri: String,
  ): OpenIdLoginStartResult {
    val parsedServer =
      uiState.normalizedServer?.let(AudiobookshelfBaseUrl::parse)
        ?: AudiobookshelfBaseUrl.parse(uiState.server)
        ?: return OpenIdLoginStartResult(
          uiState =
            uiState.copy(
              loginState = GenericState.Idle,
              serverFieldError = LoginFieldError.InvalidServerUrl,
            )
        )

    val normalizedServer = parsedServer.value
    val state = generateState()
    val codeVerifier = generateCodeVerifier()
    val codeChallenge = codeChallenge(codeVerifier)
    pendingOpenIdLoginStore.clear()
    pendingOpenIdCallbackStore.clear()
    openIdLoginFailureStore.clear()

    val authorizationUrl =
      fetchOpenIdAuthorizationUrl(
          parsedServer = parsedServer,
          redirectUri = redirectUri,
          state = state,
          codeChallenge = codeChallenge,
        )
        .getOrElse { error ->
          return OpenIdLoginStartResult(
            uiState =
              uiState.copy(
                server = normalizedServer,
                normalizedServer = normalizedServer,
                loginState = GenericState.Failure(error.message),
                serverFieldError = null,
              )
          )
        }

    pendingOpenIdLoginStore.save(
      PendingOpenIdLogin(
        normalizedServer = normalizedServer,
        serverAccessMode = uiState.serverAccessMode,
        state = state,
        codeVerifier = codeVerifier,
        createdAtEpochMillis = System.currentTimeMillis(),
      )
    )

    return OpenIdLoginStartResult(
      uiState =
        uiState.copy(
          server = normalizedServer,
          normalizedServer = normalizedServer,
          loginState = GenericState.Idle,
          serverFieldError = null,
        ),
      authorizationUrl = authorizationUrl,
    )
  }

  suspend fun openIdLoginRecoveryState(): OpenIdLoginRecoveryState {
    val pendingCallback = pendingOpenIdCallbackStore.current()
    val pendingLogin = pendingOpenIdLoginStore.current()
    return OpenIdLoginRecoveryState(
      normalizedServer = pendingCallback?.normalizedServer ?: pendingLogin?.normalizedServer,
      serverAccessMode = pendingLogin?.serverAccessMode ?: ServerAccessMode.Internet,
      hasPendingCallback = pendingCallback != null,
    )
  }

  suspend fun completeOpenIdLogin(
    nowMillis: Long = System.currentTimeMillis()
  ): OpenIdLoginCompletionResult {
    val pendingCallback = pendingOpenIdCallbackStore.current()
    val pendingLogin = pendingOpenIdLoginStore.current()

    if (pendingCallback == null) {
      return failOpenIdLoginCompletion(
        normalizedServer = pendingLogin?.normalizedServer,
        serverAccessMode = pendingLogin?.serverAccessMode ?: ServerAccessMode.Internet,
        errorMessage = "OpenID login failed because the callback is no longer available.",
      )
    }

    if (pendingLogin == null) {
      return failOpenIdLoginCompletion(
        normalizedServer = pendingCallback.normalizedServer,
        errorMessage = "OpenID login failed because the login context is no longer available.",
      )
    }

    if (pendingLogin.isExpired(nowMillis)) {
      return failOpenIdLoginCompletion(
        normalizedServer = pendingLogin.normalizedServer,
        serverAccessMode = pendingLogin.serverAccessMode,
        errorMessage =
          "OpenID login expired before the callback could be completed. Please try again.",
      )
    }

    if (
      pendingLogin.normalizedServer != pendingCallback.normalizedServer ||
        pendingLogin.state != pendingCallback.state
    ) {
      return failOpenIdLoginCompletion(
        normalizedServer = pendingLogin.normalizedServer,
        serverAccessMode = pendingLogin.serverAccessMode,
        errorMessage =
          "OpenID login failed because the callback no longer matches the current login.",
      )
    }

    val parsedServer =
      AudiobookshelfBaseUrl.parse(pendingLogin.normalizedServer)
        ?: return failOpenIdLoginCompletion(
          normalizedServer = pendingLogin.normalizedServer,
          serverAccessMode = pendingLogin.serverAccessMode,
          errorMessage = "OpenID login failed because the saved server is invalid.",
        )

    val response =
      api.openIdCallback(
        url = parsedServer.resolve("/auth/openid/callback"),
        state = pendingCallback.state,
        code = pendingCallback.code,
        codeVerifier = pendingLogin.codeVerifier,
      )
    val result = response.getOrNull()
    if (result != null) {
      val normalizedResponse = result.normalizeOpenIdLoginResponse()
      if (normalizedResponse.user.accessToken.isBlank()) {
        return failOpenIdLoginCompletion(
          normalizedServer = pendingLogin.normalizedServer,
          serverAccessMode = pendingLogin.serverAccessMode,
          errorMessage = "OpenID login failed because the server did not return an access token.",
        )
      }
      loginSuccessHandler.onLoginSuccess(
        pendingLogin.normalizedServer,
        pendingLogin.serverAccessMode,
        normalizedResponse,
      )
      pendingOpenIdLoginStore.clear()
      pendingOpenIdCallbackStore.clear()
      openIdLoginFailureStore.clear()
      return OpenIdLoginCompletionResult.Success
    }

    return failOpenIdLoginCompletion(
      normalizedServer = pendingLogin.normalizedServer,
      serverAccessMode = pendingLogin.serverAccessMode,
      errorMessage = response.exceptionOrNull().toOpenIdLoginFailureMessage(),
    )
  }

  private suspend fun fetchOpenIdAuthorizationUrl(
    parsedServer: AudiobookshelfBaseUrl,
    redirectUri: String,
    state: String,
    codeChallenge: String,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val request =
          Request.Builder()
            .url(
              parsedServer.resolveEncoded(
                "/auth/openid",
                buildOpenIdStartQuery(
                  redirectUri = redirectUri,
                  state = state,
                  codeChallenge = codeChallenge,
                ),
              )
            )
            .tag(AnonymousRequestTag::class.java, AnonymousRequestTag)
            .build()

        openIdBootstrapClient.newCall(request).execute().use { response ->
          val location = response.header("Location")?.takeUnless { it.isBlank() }
          if (response.code !in 300..399 || location == null) {
            throw IllegalStateException(response.toOpenIdStartFailureMessage(location))
          }
          URI(parsedServer.value).resolve(location).toString()
        }
      }
    }

  private suspend fun failOpenIdLoginCompletion(
    normalizedServer: String?,
    serverAccessMode: ServerAccessMode = ServerAccessMode.Internet,
    errorMessage: String,
  ): OpenIdLoginCompletionResult.Failed {
    val failure =
      recordOpenIdLoginFailure(
        pendingOpenIdLoginStore = pendingOpenIdLoginStore,
        pendingOpenIdCallbackStore = pendingOpenIdCallbackStore,
        openIdLoginFailureStore = openIdLoginFailureStore,
        normalizedServer = normalizedServer,
        serverAccessMode = serverAccessMode,
        errorMessage = errorMessage,
      )
    return OpenIdLoginCompletionResult.Failed(failure)
  }
}

data class OpenIdLoginStartResult(
  val uiState: LoginUiState,
  val authorizationUrl: String? = null,
)

data class OpenIdLoginRecoveryState(
  val normalizedServer: String? = null,
  val serverAccessMode: ServerAccessMode = ServerAccessMode.Internet,
  val hasPendingCallback: Boolean = false,
)

sealed interface OpenIdLoginCompletionResult {
  data object Success : OpenIdLoginCompletionResult

  data class Failed(val failure: OpenIdLoginFailure) : OpenIdLoginCompletionResult
}

data class LoginUiState(
  val loginState: GenericState = GenericState.Idle,
  val server: String = "",
  val normalizedServer: String? = null,
  val serverFieldError: LoginFieldError? = null,
  val serverAccessMode: ServerAccessMode = ServerAccessMode.Internet,
  val pendingLocalNetworkAction: PendingLocalNetworkAction? = null,
  val localNetworkPermissionState: LocalNetworkPermissionState? = null,
  val username: String = "",
  val password: String = "",
  val reLogin: Boolean = false,
  val authPromptReason: AuthPromptReason? = null,
  val discoveryState: LoginDiscoveryState = LoginDiscoveryState.Idle,
  val availableLoginMethods: List<LoginMethod> = listOf(LoginMethod.Local),
  val loginDiscoveryMessage: LoginDiscoveryMessage? = null,
  val authLoginCustomMessage: String? = null,
  val authOpenIdButtonText: String? = null,
  val authOpenIdAutoLaunch: Boolean? = null,
)

sealed interface PendingLocalNetworkAction {
  data object DiscoverLoginMethods : PendingLocalNetworkAction

  data object PasswordLogin : PendingLocalNetworkAction

  data class OpenIdLoginStart(val redirectUri: String) : PendingLocalNetworkAction

  data object CompleteOpenIdLogin : PendingLocalNetworkAction
}

enum class LocalNetworkPermissionState {
  Denied,
  PermanentlyDenied,
}

enum class LoginFieldError {
  InvalidServerUrl
}

enum class LoginMethod {
  Local,
  OpenId,
}

enum class LoginDiscoveryMessage {
  MethodsUnconfirmed,
  LocalLoginUnavailable,
}

sealed interface LoginDiscoveryState {
  data object Idle : LoginDiscoveryState

  data object Loading : LoginDiscoveryState

  data object Success : LoginDiscoveryState

  data object Failure : LoginDiscoveryState
}

data class LoginDiscoveryResult(
  val normalizedServer: String? = null,
  val discoveryState: LoginDiscoveryState = LoginDiscoveryState.Idle,
  val availableLoginMethods: List<LoginMethod> = listOf(LoginMethod.Local),
  val loginDiscoveryMessage: LoginDiscoveryMessage? = null,
  val authLoginCustomMessage: String? = null,
  val authOpenIdButtonText: String? = null,
  val authOpenIdAutoLaunch: Boolean? = null,
)

fun LoginUiState.supportsLocalLogin(): Boolean {
  return discoveryState !is LoginDiscoveryState.Success ||
    LoginMethod.Local in availableLoginMethods
}

fun LoginUiState.showsMixedLoginMethods(): Boolean {
  return discoveryState is LoginDiscoveryState.Success &&
    LoginMethod.Local in availableLoginMethods &&
    LoginMethod.OpenId in availableLoginMethods &&
    supportsOpenIdLogin()
}

fun LoginUiState.isOpenIdOnly(): Boolean {
  return discoveryState is LoginDiscoveryState.Success &&
    LoginMethod.Local !in availableLoginMethods &&
    LoginMethod.OpenId in availableLoginMethods &&
    supportsOpenIdLogin()
}

private fun LoginUiState.supportsOpenIdLogin(): Boolean {
  val server = normalizedServer ?: server
  return AudiobookshelfBaseUrl.parse(server)?.scheme == "https"
}

private fun List<String>.toLoginMethods(): List<LoginMethod> {
  val normalized = map { it.trim().lowercase() }
  val methods = mutableListOf<LoginMethod>()
  if ("local" in normalized) {
    methods += LoginMethod.Local
  }
  if ("openid" in normalized) {
    methods += LoginMethod.OpenId
  }
  return methods
}

sealed interface LoginEvent {
  data object LoginButtonPressed : LoginEvent

  data class OpenIdLoginButtonPressed(val redirectUri: String) : LoginEvent

  data class LocalNetworkPermissionResult(
    val granted: Boolean,
    val permanentlyDenied: Boolean = false,
  ) : LoginEvent

  data class ServerAccessModeChanged(val serverAccessMode: ServerAccessMode) : LoginEvent

  data object UseDifferentServerOrAccountConfirmed : LoginEvent

  data object ErrorShown : LoginEvent

  data class ServerChanged(val server: String) : LoginEvent

  data class UsernameChanged(val username: String) : LoginEvent

  data class PasswordChanged(val password: String) : LoginEvent
}

private fun buildOpenIdStartQuery(
  redirectUri: String,
  state: String,
  codeChallenge: String,
): String {
  return listOf(
      "client_id" to "shelfdroid",
      "redirect_uri" to redirectUri,
      "response_type" to "code",
      "state" to state,
      "code_challenge" to codeChallenge,
      "code_challenge_method" to "S256",
    )
    .joinToString("&") { (key, value) -> "${key.encodeQueryValue()}=${value.encodeQueryValue()}" }
}

private fun generateState(): String = generateUrlSafeToken(byteCount = 16)

private fun generateCodeVerifier(): String = generateUrlSafeToken(byteCount = 32)

private fun generateUrlSafeToken(byteCount: Int): String {
  val bytes = ByteArray(byteCount)
  secureRandom.nextBytes(bytes)
  return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun codeChallenge(codeVerifier: String): String {
  val digest =
    MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
  return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
}

private fun String.encodeQueryValue(): String =
  URLEncoder.encode(this, StandardCharsets.UTF_8).replace("+", "%20")

private fun okhttp3.Response.toOpenIdStartFailureMessage(location: String?): String {
  if (location == null && code in 300..399) {
    return "OpenID login failed because the server did not provide an authorization URL."
  }
  val responseMessage = body?.string()?.trim().orEmpty()
  return when {
    responseMessage.isNotEmpty() -> responseMessage
    message.isNotBlank() -> message
    else -> "OpenID login failed while starting the browser sign-in."
  }
}

private fun Throwable?.toOpenIdLoginFailureMessage(): String {
  return when (this) {
    is HttpException ->
      when (code()) {
        400,
        401 -> "OpenID login failed. Please try again."
        404 -> "Server not found."
        429 -> "Too many requests. Please wait and try again."
        else -> message()
      }

    null -> "OpenID login failed. Please try again."
    else -> message ?: "OpenID login failed. Please try again."
  }
}

private fun LoginResponse.normalizeOpenIdLoginResponse(): LoginResponse {
  if (user.accessToken.isNotBlank() || user.token.isBlank()) {
    return this
  }
  return copy(user = user.copy(accessToken = user.token))
}

private val secureRandom = SecureRandom()
