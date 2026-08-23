package dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilsContract
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataApiState
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataDialog
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataOperation
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataUiState
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationError
import dev.halim.shelfdroid.core.data.metadata.custommetadata.MetadataValidationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CustomMetadataViewModel @Inject constructor(private val repository: MetadataUtilsContract) :
  ViewModel() {
  private val _uiState = MutableStateFlow(CustomMetadataUiState())
  val uiState: StateFlow<CustomMetadataUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CustomMetadataUiState(),
      )

  fun onEvent(event: CustomMetadataEvent) {
    when (event) {
      CustomMetadataEvent.Load,
      CustomMetadataEvent.Retry -> load()
      is CustomMetadataEvent.UpdateName -> _uiState.update { it.copy(nameDraft = event.value) }
      is CustomMetadataEvent.UpdateUrl -> _uiState.update { it.copy(urlDraft = event.value) }
      is CustomMetadataEvent.UpdateAuthHeader ->
        _uiState.update { it.copy(authHeaderDraft = event.value) }
      is CustomMetadataEvent.SetAuthHeaderVisible ->
        _uiState.update { it.copy(authHeaderVisible = event.visible) }
      is CustomMetadataEvent.SetProviderVisible ->
        if (_uiState.value.providers.any { it.id == event.providerId }) {
          _uiState.update {
            it.copy(
              revealedProviderIds =
                if (event.visible) it.revealedProviderIds + event.providerId
                else it.revealedProviderIds - event.providerId
            )
          }
        }
      CustomMetadataEvent.SubmitCreate -> create()
      is CustomMetadataEvent.BeginDelete ->
        if (!_uiState.value.isMutating) {
          _uiState.update {
            it.copy(
              // The confirmation only needs identity and name; do not carry the provider
              // authorization header into another transient state object.
              dialog = CustomMetadataDialog.Delete(event.provider.copy(authHeaderValue = null)),
              apiState = CustomMetadataApiState.Idle,
            )
          }
        }
      CustomMetadataEvent.DismissDialog -> _uiState.update { it.copy(dialog = null) }
      CustomMetadataEvent.ConfirmDelete -> delete()
      CustomMetadataEvent.ClearApiState ->
        _uiState.update { it.copy(apiState = CustomMetadataApiState.Idle) }
      CustomMetadataEvent.ClearSensitiveState -> clearSensitiveState()
    }
  }

  private fun load() {
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          state = GenericState.Loading,
          apiState = CustomMetadataApiState.Loading,
        )
      }
      repository
        .loadCustomMetadataProviders()
        .fold(
          onSuccess = { providers ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = CustomMetadataApiState.Idle,
                providers = providers,
              )
            }
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                state = GenericState.Failure(error.message),
                apiState = failureState(error),
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
      showValidationFailure(MetadataValidationError.CustomMetadataProviderNameRequired)
      return
    }
    if (url.isBlank()) {
      showValidationFailure(MetadataValidationError.CustomMetadataProviderUrlRequired)
      return
    }
    if (state.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(apiState = CustomMetadataApiState.Mutating(CustomMetadataOperation.Create))
      }
      repository
        .createCustomMetadataProvider(name, url, state.authHeaderDraft)
        .fold(
          onSuccess = {
            _uiState.update {
              it.copy(
                nameDraft = "",
                urlDraft = "",
                authHeaderDraft = "",
                authHeaderVisible = false,
                apiState = CustomMetadataApiState.CreateSuccess,
              )
            }
            reloadAfterMutation(CustomMetadataOperation.Create)
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(apiState = failureState(error, CustomMetadataOperation.Create))
            }
          },
        )
    }
  }

  private fun delete() {
    val provider = (_uiState.value.dialog as? CustomMetadataDialog.Delete)?.provider ?: return
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          dialog = null,
          apiState = CustomMetadataApiState.Mutating(CustomMetadataOperation.Delete),
        )
      }
      repository
        .deleteCustomMetadataProvider(provider.id)
        .fold(
          onSuccess = {
            _uiState.update {
              it.copy(
                apiState = CustomMetadataApiState.DeleteSuccess,
                revealedProviderIds = it.revealedProviderIds - provider.id,
              )
            }
            reloadAfterMutation(CustomMetadataOperation.Delete)
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(apiState = failureState(error, CustomMetadataOperation.Delete))
            }
          },
        )
    }
  }

  private suspend fun reloadAfterMutation(operation: CustomMetadataOperation) {
    repository
      .loadCustomMetadataProviders()
      .fold(
        onSuccess = { providers ->
          _uiState.update {
            it.copy(providers = providers)
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(apiState = failureState(error, operation))
          }
        },
      )
  }

  private fun showValidationFailure(error: MetadataValidationError) {
    _uiState.update {
      it.copy(
        apiState =
          CustomMetadataApiState.Failure(
            validationError = error,
            operation = CustomMetadataOperation.Create,
          )
      )
    }
  }

  private fun failureState(
    error: Throwable,
    operation: CustomMetadataOperation? = null,
  ): CustomMetadataApiState.Failure =
    CustomMetadataApiState.Failure(
      serverDetail = error.message,
      validationError = (error as? MetadataValidationException)?.error,
      operation = operation,
    )

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

sealed interface CustomMetadataEvent {
  data object Load : CustomMetadataEvent

  data object Retry : CustomMetadataEvent

  data class UpdateName(val value: String) : CustomMetadataEvent

  data class UpdateUrl(val value: String) : CustomMetadataEvent

  data class UpdateAuthHeader(val value: String) : CustomMetadataEvent

  data class SetAuthHeaderVisible(val visible: Boolean) : CustomMetadataEvent

  data class SetProviderVisible(val providerId: String, val visible: Boolean) : CustomMetadataEvent

  data object SubmitCreate : CustomMetadataEvent

  data class BeginDelete(val provider: CustomMetadataProvider) : CustomMetadataEvent

  data object DismissDialog : CustomMetadataEvent

  data object ConfirmDelete : CustomMetadataEvent

  data object ClearApiState : CustomMetadataEvent

  data object ClearSensitiveState : CustomMetadataEvent
}
