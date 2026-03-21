package dev.halim.shelfdroid.core.data.screen.usersettings.changepassword

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.ChangePasswordRequest
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import retrofit2.HttpException

class ChangePasswordRepository @Inject constructor(private val api: ApiService) {

  suspend fun changePassword(
    uiState: ChangePasswordUiState,
    event: MutableSharedFlow<ChangePasswordUiEvent>,
  ): ChangePasswordUiState {
    val request = ChangePasswordRequest(uiState.old, uiState.new)
    val response = api.changePassword(request)
    val result = response.getOrElse {
      val isInvalid = if (it is HttpException) it.code() == 400 else false
      event.emit(ChangePasswordUiEvent.ApiError(isInvalid, it.message))
      return uiState.copy(state = GenericState.Failure())
    }
    event.emit(ChangePasswordUiEvent.Success)
    return uiState.copy(state = GenericState.Success, old = "", new = "", confirm = "")
  }
}
