package dev.halim.shelfdroid.core.data.screen.apikeys.edit

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.UpdateApiKeyRequest
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.response.UserRepo
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow

class EditApiKeysRepository
@Inject
constructor(private val api: ApiService, private val userRepo: UserRepo) {

  fun item(nav: NavEditApiKeys): EditApiKeysUiState {
    val users = userRepo.all().map { EditApiKeysUiState.User(it.id, it.username) }
    return EditApiKeysUiState(
      state = GenericState.Success,
      apiKeyId = nav.id,
      name = nav.name,
      users = users,
      selectedUserId = nav.userId,
      isActive = nav.isActive,
    )
  }

  suspend fun updateApiKey(
    uiState: EditApiKeysUiState,
    events: MutableSharedFlow<GenericUiEvent>,
  ): EditApiKeysUiState {
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
