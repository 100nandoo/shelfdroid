package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateNavigation
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateResult
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.validateLibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.normalizeLibraryFolderPath
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryAdministrationCreateViewModel
@Inject
constructor(private val repository: LibraryAdministrationCreateContract) : ViewModel() {

  private val _uiState = MutableStateFlow(LibraryAdministrationCreateUiState())
  val uiState: StateFlow<LibraryAdministrationCreateUiState> =
    _uiState
      .onStart { loadProviders(LibraryAdministrationMediaType.BOOK) }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LibraryAdministrationCreateUiState(),
      )

  fun onEvent(event: LibraryAdministrationCreateEvent) {
    when (event) {
      LibraryAdministrationCreateEvent.Load -> loadProviders(_uiState.value.draft.mediaType)
      LibraryAdministrationCreateEvent.RetryProviders -> loadProviders(_uiState.value.draft.mediaType)
      is LibraryAdministrationCreateEvent.SelectMediaType -> selectMediaType(event.mediaType)
      is LibraryAdministrationCreateEvent.UpdateName -> updateDraft { copy(name = event.value) }
      is LibraryAdministrationCreateEvent.SelectIcon -> updateDraft { copy(icon = event.icon) }
      is LibraryAdministrationCreateEvent.SelectProvider -> updateDraft { withProvider(event.providerId) }
      is LibraryAdministrationCreateEvent.SelectTab ->
        _uiState.update { it.copy(selectedTab = event.tab) }
      is LibraryAdministrationCreateEvent.UpdateManualFolder ->
        _uiState.update { it.copy(manualFolderDraft = event.value, isDirty = true) }
      LibraryAdministrationCreateEvent.AddManualFolder -> addManualFolder()
      is LibraryAdministrationCreateEvent.SelectFolder -> addFolder(event.path)
      is LibraryAdministrationCreateEvent.RemoveFolder ->
        updateDraft { copy(folders = folders.filterNot { it == event.path }) }
      LibraryAdministrationCreateEvent.OpenFilesystem -> browseFilesystem(null)
      is LibraryAdministrationCreateEvent.OpenFilesystemPath -> browseFilesystem(event.path)
      LibraryAdministrationCreateEvent.CloseFilesystem ->
        _uiState.update { it.copy(filesystemState = LibraryAdministrationFilesystemState.Closed) }
      LibraryAdministrationCreateEvent.Submit -> submit()
      LibraryAdministrationCreateEvent.RetryLocalSynchronization -> retryLocalSynchronization()
      LibraryAdministrationCreateEvent.Back -> handleBack()
      LibraryAdministrationCreateEvent.ConfirmDiscard ->
        _uiState.update {
          it.copy(navigation = LibraryAdministrationCreateNavigation.Back, discardDialog = false)
        }
      LibraryAdministrationCreateEvent.CancelDiscard ->
        _uiState.update { it.copy(discardDialog = false) }
      LibraryAdministrationCreateEvent.ConsumeNavigation ->
        _uiState.update { it.copy(navigation = null) }
      LibraryAdministrationCreateEvent.ConsumeFocus ->
        _uiState.update { it.copy(focusField = null) }
    }
  }

  private fun selectMediaType(mediaType: LibraryAdministrationMediaType) {
    if (mediaType == _uiState.value.draft.mediaType) return
    _uiState.update {
      it.copy(
        draft = it.draft.withMediaType(mediaType),
        providerState = LibraryAdministrationProviderState.Loading,
        selectedTab =
          if (mediaType == LibraryAdministrationMediaType.PODCAST &&
              it.selectedTab == LibraryAdministrationCreateTab.SCANNER) {
            LibraryAdministrationCreateTab.DETAILS
          } else {
            it.selectedTab
          },
        validation = it.validation.copy(errors = it.validation.errors - LibraryAdministrationCreateField.PROVIDER),
        isDirty = true,
      )
    }
    loadProviders(mediaType)
  }

  private fun loadProviders(mediaType: LibraryAdministrationMediaType) {
    if (_uiState.value.isSubmitting) return
    _uiState.update { it.copy(providerState = LibraryAdministrationProviderState.Loading) }
    viewModelScope.launch {
      repository.loadLibraryProviders(mediaType).fold(
        onSuccess = { providers ->
          _uiState.update { state ->
            if (state.draft.mediaType != mediaType) state
            else {
              val selected = state.draft.provider
              val provider = selected?.takeIf { value -> providers.any { it.id == value } } ?: providers.firstOrNull()?.id
              state.copy(
                draft = state.draft.withProvider(provider),
                providerState = LibraryAdministrationProviderState.Success(providers),
              )
            }
          }
        },
        onFailure = { _ ->
          _uiState.update { state ->
            if (state.draft.mediaType == mediaType) {
              state.copy(providerState = LibraryAdministrationProviderState.Failure(null))
            } else state
          }
        },
      )
    }
  }

  private fun browseFilesystem(path: String?) {
    if (_uiState.value.isSubmitting) return
    _uiState.update { it.copy(filesystemState = LibraryAdministrationFilesystemState.Loading(path)) }
    viewModelScope.launch {
      repository.browseLibraryFilesystem(path).fold(
        onSuccess = { filesystem ->
          _uiState.update {
            it.copy(filesystemState = LibraryAdministrationFilesystemState.Success(path, filesystem))
          }
        },
        onFailure = { _ ->
          _uiState.update {
            it.copy(filesystemState = LibraryAdministrationFilesystemState.Failure(path, null))
          }
        },
      )
    }
  }

  private fun addManualFolder() {
    val path = normalizeLibraryFolderPath(_uiState.value.manualFolderDraft)
    if (path.isBlank()) return
    addFolder(path)
    _uiState.update { it.copy(manualFolderDraft = "") }
  }

  private fun addFolder(path: String) {
    val normalized = normalizeLibraryFolderPath(path)
    if (normalized.isBlank() || _uiState.value.draft.folders.contains(normalized)) return
    updateDraft { copy(folders = folders + normalized) }
  }

  private fun updateDraft(update: LibraryAdministrationDraft.() -> LibraryAdministrationDraft) {
    _uiState.update {
      it.copy(
        draft = update(it.draft),
        isDirty = true,
        validation = it.validation.copy(errors = emptyMap()),
      )
    }
  }

  private fun submit() {
    val state = _uiState.value
    if (state.isSubmitting) return
    val validation =
      validateLibraryAdministrationDraft(
        draft = state.draft,
        providers = (state.providerState as? LibraryAdministrationProviderState.Success)?.providers,
      )
    if (!validation.isValid) {
      _uiState.update {
        it.copy(
          selectedTab = LibraryAdministrationCreateTab.DETAILS,
          validation = validation,
          focusField = validation.firstInvalidField,
        )
      }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(submissionState = LibraryAdministrationCreateSubmissionState.Submitting) }
      repository.createLibrary(state.draft).fold(
        onSuccess = { result ->
          _uiState.update {
            when (result) {
              is LibraryAdministrationCreateResult.Created ->
                it.copy(
                  submissionState = LibraryAdministrationCreateSubmissionState.Idle,
                  navigation = LibraryAdministrationCreateNavigation.Created(result.library),
                )
              is LibraryAdministrationCreateResult.CreatedButNotSynchronized ->
                it.copy(
                  submissionState =
                    LibraryAdministrationCreateSubmissionState.LocalSyncFailure(
                      result.library,
                      null,
                    )
                )
            }
          }
        },
        onFailure = { _ ->
          _uiState.update {
            it.copy(
              submissionState = LibraryAdministrationCreateSubmissionState.ServerFailure(null)
            )
          }
        },
      )
    }
  }

  private fun retryLocalSynchronization() {
    if (_uiState.value.isSubmitting) return
    val localFailure =
      _uiState.value.submissionState as? LibraryAdministrationCreateSubmissionState.LocalSyncFailure
        ?: return
    viewModelScope.launch {
      _uiState.update { it.copy(submissionState = LibraryAdministrationCreateSubmissionState.Submitting) }
      repository.synchronizeLibraries().fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              submissionState = LibraryAdministrationCreateSubmissionState.Idle,
              navigation = LibraryAdministrationCreateNavigation.Created(localFailure.library),
            )
          }
        },
        onFailure = { _ ->
          _uiState.update {
            it.copy(
              submissionState =
                LibraryAdministrationCreateSubmissionState.LocalSyncFailure(
                  localFailure.library,
                  null,
                )
            )
          }
        },
      )
    }
  }

  private fun handleBack() {
    if (_uiState.value.isDirty) {
      _uiState.update { it.copy(discardDialog = true) }
    } else {
      _uiState.update { it.copy(navigation = LibraryAdministrationCreateNavigation.Back) }
    }
  }
}

sealed interface LibraryAdministrationCreateEvent {
  data object Load : LibraryAdministrationCreateEvent
  data object RetryProviders : LibraryAdministrationCreateEvent
  data class SelectMediaType(val mediaType: LibraryAdministrationMediaType) : LibraryAdministrationCreateEvent
  data class UpdateName(val value: String) : LibraryAdministrationCreateEvent
  data class SelectIcon(val icon: String) : LibraryAdministrationCreateEvent
  data class SelectProvider(val providerId: String) : LibraryAdministrationCreateEvent
  data class SelectTab(val tab: LibraryAdministrationCreateTab) : LibraryAdministrationCreateEvent
  data class UpdateManualFolder(val value: String) : LibraryAdministrationCreateEvent
  data object AddManualFolder : LibraryAdministrationCreateEvent
  data class SelectFolder(val path: String) : LibraryAdministrationCreateEvent
  data class RemoveFolder(val path: String) : LibraryAdministrationCreateEvent
  data object OpenFilesystem : LibraryAdministrationCreateEvent
  data class OpenFilesystemPath(val path: String) : LibraryAdministrationCreateEvent
  data object CloseFilesystem : LibraryAdministrationCreateEvent
  data object Submit : LibraryAdministrationCreateEvent
  data object RetryLocalSynchronization : LibraryAdministrationCreateEvent
  data object Back : LibraryAdministrationCreateEvent
  data object ConfirmDiscard : LibraryAdministrationCreateEvent
  data object CancelDiscard : LibraryAdministrationCreateEvent
  data object ConsumeNavigation : LibraryAdministrationCreateEvent
  data object ConsumeFocus : LibraryAdministrationCreateEvent
}
