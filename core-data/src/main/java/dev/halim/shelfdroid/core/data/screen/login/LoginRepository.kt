package dev.halim.shelfdroid.core.data.screen.login

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.LoginRequest
import dev.halim.core.network.response.LoginResponse
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import retrofit2.HttpException

class LoginRepository
@Inject
constructor(
  private val api: ApiService,
  private val mapper: LoginMapper,
  private val dataStoreManager: DataStoreManager,
  prefsRepository: PrefsRepository,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
) {

  val userPrefs = prefsRepository.userPrefs
  val baseUrl = dataStoreManager.baseUrl()

  suspend fun login(uiState: LoginUiState): LoginUiState {
    DataStoreManager.BASE_URL = uiState.server
    val request = LoginRequest(uiState.username, uiState.password)
    val response = api.login(request)
    val result = response.getOrNull()
    if (result != null) {
      updateDataStoreManager(uiState.server, result)
      progressRepo.saveAndConvert(result.user)
      bookmarkRepo.saveAndConvert(result.user)
      return uiState.copy(loginState = GenericState.Success)
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
    return uiState.copy(loginState = GenericState.Failure(message))
  }

  private suspend fun updateDataStoreManager(server: String, response: LoginResponse) {
    dataStoreManager.apply {
      val userPrefs = mapper.toUserPrefs(response.user)

      updateBaseUrl(server)
      updateUserPrefs(userPrefs)
    }
  }
}

data class LoginUiState(
  val loginState: GenericState = GenericState.Idle,
  val server: String = "",
  val username: String = "",
  val password: String = "",
  val reLogin: Boolean = false,
)

sealed class LoginEvent {
  data object LoginButtonPressed : LoginEvent()

  data object ErrorShown : LoginEvent()

  data class ServerChanged(val server: String) : LoginEvent()

  data class UsernameChanged(val username: String) : LoginEvent()

  data class PasswordChanged(val password: String) : LoginEvent()
}
