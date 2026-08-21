package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementApiState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementDialog
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementUiState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderNameRequiredException
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderOperation
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderUrlRequiredException
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CustomMetadataProviderManagementViewModel
@Inject
constructor(private val repository: MetadataUtilitiesRepositoryContract) : ViewModel() {
  private val _uiState = MutableStateFlow(CustomMetadataProviderManagementUiState())
  val uiState: StateFlow<CustomMetadataProviderManagementUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CustomMetadataProviderManagementUiState(),
      )

  fun onEvent(event: CustomMetadataProviderManagementEvent) {
    when (event) {
      CustomMetadataProviderManagementEvent.Load,
      CustomMetadataProviderManagementEvent.Retry -> load()
      is CustomMetadataProviderManagementEvent.UpdateName ->
        _uiState.update { it.copy(nameDraft = event.value) }
      is CustomMetadataProviderManagementEvent.UpdateUrl ->
        _uiState.update { it.copy(urlDraft = event.value) }
      is CustomMetadataProviderManagementEvent.UpdateAuthHeader ->
        _uiState.update { it.copy(authHeaderDraft = event.value) }
      is CustomMetadataProviderManagementEvent.SetAuthHeaderVisible ->
        _uiState.update { it.copy(authHeaderVisible = event.visible) }
      is CustomMetadataProviderManagementEvent.SetProviderVisible ->
        if (_uiState.value.providers.any { it.id == event.providerId }) {
          _uiState.update {
            it.copy(
              revealedProviderIds =
                if (event.visible) it.revealedProviderIds + event.providerId
                else it.revealedProviderIds - event.providerId
            )
          }
        }
      CustomMetadataProviderManagementEvent.SubmitCreate -> create()
      is CustomMetadataProviderManagementEvent.BeginDelete ->
        if (!_uiState.value.isMutating) {
          _uiState.update {
            it.copy(
              // The confirmation only needs identity and name; do not carry the provider
              // authorization header into another transient state object.
              dialog =
                CustomMetadataProviderManagementDialog.Delete(
                  event.provider.copy(authHeaderValue = null)
                ),
              apiState = CustomMetadataProviderManagementApiState.Idle,
            )
          }
        }
      CustomMetadataProviderManagementEvent.DismissDialog ->
        _uiState.update { it.copy(dialog = null) }
      CustomMetadataProviderManagementEvent.ConfirmDelete -> delete()
      CustomMetadataProviderManagementEvent.ClearApiState ->
        _uiState.update { it.copy(apiState = CustomMetadataProviderManagementApiState.Idle) }
      CustomMetadataProviderManagementEvent.ClearSensitiveState -> clearSensitiveState()
    }
  }

  private fun load() {
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          state = GenericState.Loading,
          apiState = CustomMetadataProviderManagementApiState.Loading,
        )
      }
      repository.loadCustomMetadataProviders().fold(
        onSuccess = { providers ->
          _uiState.update {
            it.copy(
              state = GenericState.Success,
              apiState = CustomMetadataProviderManagementApiState.Idle,
              providers = providers.filter { provider -> provider.mediaType == BOOK_MEDIA_TYPE },
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              state = GenericState.Failure(error.message),
              apiState =
                CustomMetadataProviderManagementApiState.Failure(
                  error.message,
                  error is MetadataAccessDeniedException,
                ),
            )
          }
        },
      )
    }
  }

  private fun create() {
    val state = _uiState.value
    val name = state.nameDraft.trim()
    val url = state.urlDraft.trim()
    if (name.isBlank()) {
      showValidationFailure(CustomMetadataProviderNameRequiredException())
      return
    }
    if (url.isBlank()) {
      showValidationFailure(CustomMetadataProviderUrlRequiredException())
      return
    }
    if (state.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState = CustomMetadataProviderManagementApiState.Mutating(
            CustomMetadataProviderOperation.Create
          )
        )
      }
      repository.createCustomMetadataProvider(name, url, state.authHeaderDraft).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              nameDraft = "",
              urlDraft = "",
              authHeaderDraft = "",
              authHeaderVisible = false,
              apiState = CustomMetadataProviderManagementApiState.CreateSuccess,
            )
          }
          reloadAfterMutation(CustomMetadataProviderOperation.Create)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              apiState =
                CustomMetadataProviderManagementApiState.Failure(
                  error.message,
                  error is MetadataAccessDeniedException,
                  CustomMetadataProviderOperation.Create,
                ),
            )
          }
        },
      )
    }
  }

  private fun delete() {
    val provider =
      (_uiState.value.dialog as? CustomMetadataProviderManagementDialog.Delete)?.provider
        ?: return
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          dialog = null,
          apiState = CustomMetadataProviderManagementApiState.Mutating(
            CustomMetadataProviderOperation.Delete
          ),
        )
      }
      repository.deleteCustomMetadataProvider(provider.id).fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              apiState = CustomMetadataProviderManagementApiState.DeleteSuccess,
              revealedProviderIds = it.revealedProviderIds - provider.id,
            )
          }
          reloadAfterMutation(CustomMetadataProviderOperation.Delete)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              apiState =
                CustomMetadataProviderManagementApiState.Failure(
                  error.message,
                  error is MetadataAccessDeniedException,
                  CustomMetadataProviderOperation.Delete,
                ),
            )
          }
        },
      )
    }
  }

  private suspend fun reloadAfterMutation(operation: CustomMetadataProviderOperation) {
    repository.loadCustomMetadataProviders().fold(
      onSuccess = { providers ->
        _uiState.update {
          it.copy(
            providers = providers.filter { provider -> provider.mediaType == BOOK_MEDIA_TYPE }
          )
        }
      },
      onFailure = { error ->
        _uiState.update {
          it.copy(
            apiState =
              CustomMetadataProviderManagementApiState.Failure(
                error.message,
                error is MetadataAccessDeniedException,
                operation,
              )
          )
        }
      },
    )
  }

  private fun showValidationFailure(error: Throwable) {
    _uiState.update {
      it.copy(
        apiState = CustomMetadataProviderManagementApiState.Failure(
          error.message,
          operation = CustomMetadataProviderOperation.Create,
        )
      )
    }
  }

  private fun clearSensitiveState() {
    _uiState.update {
      it.copy(
        providers = it.providers.map { provider -> provider.copy(authHeaderValue = null) },
        nameDraft = "",
        urlDraft = "",
        authHeaderDraft = "",
        authHeaderVisible = false,
        revealedProviderIds = emptySet(),
      )
    }
  }
}

private const val BOOK_MEDIA_TYPE = "book"

sealed interface CustomMetadataProviderManagementEvent {
  data object Load : CustomMetadataProviderManagementEvent
  data object Retry : CustomMetadataProviderManagementEvent
  data class UpdateName(val value: String) : CustomMetadataProviderManagementEvent
  data class UpdateUrl(val value: String) : CustomMetadataProviderManagementEvent
  data class UpdateAuthHeader(val value: String) : CustomMetadataProviderManagementEvent
  data class SetAuthHeaderVisible(val visible: Boolean) : CustomMetadataProviderManagementEvent
  data class SetProviderVisible(val providerId: String, val visible: Boolean) :
    CustomMetadataProviderManagementEvent
  data object SubmitCreate : CustomMetadataProviderManagementEvent
  data class BeginDelete(val provider: CustomMetadataProvider) :
    CustomMetadataProviderManagementEvent
  data object DismissDialog : CustomMetadataProviderManagementEvent
  data object ConfirmDelete : CustomMetadataProviderManagementEvent
  data object ClearApiState : CustomMetadataProviderManagementEvent
  data object ClearSensitiveState : CustomMetadataProviderManagementEvent
}
