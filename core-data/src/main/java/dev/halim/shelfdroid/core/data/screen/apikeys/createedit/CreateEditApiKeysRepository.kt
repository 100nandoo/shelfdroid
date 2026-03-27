package dev.halim.shelfdroid.core.data.screen.apikeys.createedit

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateApiKeyRequest
import dev.halim.core.network.request.UpdateApiKeyRequest
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

class CreateEditApiKeysRepository
@Inject
constructor(private val api: ApiService, private val userRepo: UserRepo) {

  fun item(nav: NavEditApiKeys): CreateEditApiKeysUiState {
    val users = userRepo.all().map { CreateEditApiKeysUiState.User(it.id, it.username) }
    return CreateEditApiKeysUiState(
      state = GenericState.Success,
      apiKeyId = nav.id,
      name = nav.name,
      users = users,
      selectedUserId = nav.userId,
      isActive = nav.isActive,
    )
  }

  suspend fun createApiKey(
    uiState: CreateEditApiKeysUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): CreateEditApiKeysUiState {
    val expiresIn =
      if (uiState.neverExpires) {
        0
      } else {
        ((uiState.expiresAtMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
      }
    val request =
      CreateApiKeyRequest(
        name = uiState.name,
        expiresIn = expiresIn,
        isActive = uiState.isActive,
        userId = uiState.selectedUserId,
      )
    api.createApiKey(request).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return uiState.copy(state = GenericState.Failure(it.message))
    }
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    events.emit(GenericUiEvent.NavigateBack)
    return uiState.copy(state = GenericState.Success)
  }

  suspend fun updateApiKey(
    uiState: CreateEditApiKeysUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): CreateEditApiKeysUiState {
    val request = UpdateApiKeyRequest(isActive = uiState.isActive, userId = uiState.selectedUserId)
    api.updateApiKey(uiState.apiKeyId, request).getOrElse {
      events.emit(GenericUiEvent.ShowErrorSnackbar(it.message.orEmpty()))
      return uiState.copy(state = GenericState.Failure(it.message))
    }
    events.emit(GenericUiEvent.ShowSuccessSnackbar())
    events.emit(GenericUiEvent.NavigateBack)
    return uiState.copy(state = GenericState.Success)
  }
}
