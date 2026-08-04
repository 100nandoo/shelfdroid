package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.LoginRequest
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import retrofit2.HttpException

class LoginRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  prefsRepository: PrefsRepository,
  private val loginSuccessHandler: LoginSuccessHandler,
  private val pendingOpenIdLoginStore: PendingOpenIdLoginStore,
  private val pendingOpenIdCallbackStore: PendingOpenIdCallbackStore,
  private val openIdLoginFailureStore: OpenIdLoginFailureStore,
) {

  val userPrefs = prefsRepository.userPrefs
  val baseUrl = dataStoreManager.baseUrl()

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
      loginSuccessHandler.onLoginSuccess(normalizedServer, result)
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
    pendingOpenIdCallbackStore.clear()
    openIdLoginFailureStore.clear()
    pendingOpenIdLoginStore.save(
      PendingOpenIdLogin(
        normalizedServer = normalizedServer,
        state = state,
        codeVerifier = codeVerifier,
        createdAtEpochMillis = System.currentTimeMillis(),
      )
    )

    val authorizationUrl =
      parsedServer.resolveEncoded(
        "/auth/openid",
        buildOpenIdStartQuery(
          redirectUri = redirectUri,
          state = state,
          codeChallenge = codeChallenge,
        ),
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
}

data class OpenIdLoginStartResult(
  val uiState: LoginUiState,
  val authorizationUrl: String? = null,
)

data class LoginUiState(
  val loginState: GenericState = GenericState.Idle,
  val server: String = "",
  val normalizedServer: String? = null,
  val serverFieldError: LoginFieldError? = null,
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

fun LoginUiState.supportsOpenIdLogin(): Boolean = LoginMethod.OpenId in availableLoginMethods

fun LoginUiState.showsMixedLoginMethods(): Boolean {
  return discoveryState is LoginDiscoveryState.Success &&
    LoginMethod.Local in availableLoginMethods &&
    LoginMethod.OpenId in availableLoginMethods
}

fun LoginUiState.isOpenIdOnly(): Boolean {
  return discoveryState is LoginDiscoveryState.Success &&
    LoginMethod.Local !in availableLoginMethods &&
    LoginMethod.OpenId in availableLoginMethods
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

private val secureRandom = SecureRandom()
