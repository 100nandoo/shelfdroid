package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.LoginRequest
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import retrofit2.HttpException

class LoginRepository
@Inject
constructor(
  private val api: ApiService,
  private val dataStoreManager: DataStoreManager,
  prefsRepository: PrefsRepository,
  private val loginSuccessHandler: LoginSuccessHandler,
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
}

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

  data object UseDifferentServerOrAccountConfirmed : LoginEvent

  data object ErrorShown : LoginEvent

  data class ServerChanged(val server: String) : LoginEvent

  data class UsernameChanged(val username: String) : LoginEvent

  data class PasswordChanged(val password: String) : LoginEvent
}
