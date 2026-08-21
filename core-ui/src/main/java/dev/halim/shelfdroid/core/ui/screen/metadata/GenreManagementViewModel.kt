package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.GenreManagementApiState
import dev.halim.shelfdroid.core.data.metadata.GenreManagementDialog
import dev.halim.shelfdroid.core.data.metadata.GenreManagementUiState
import dev.halim.shelfdroid.core.data.metadata.GenreOperation
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract
import dev.halim.shelfdroid.core.data.metadata.sortedGenres
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GenreManagementViewModel
@Inject
constructor(private val repository: MetadataUtilitiesRepositoryContract) : ViewModel() {
  private val _uiState = MutableStateFlow(GenreManagementUiState())
  val uiState: StateFlow<GenreManagementUiState> =
    _uiState
      .onStart { load() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenreManagementUiState())

  fun onEvent(event: GenreManagementEvent) {
    when (event) {
      GenreManagementEvent.Load, GenreManagementEvent.Retry -> load()
      is GenreManagementEvent.BeginRename ->
        if (!_uiState.value.isMutating) {
          _uiState.update {
            it.copy(
              editingGenre = event.genre,
              renameDraft = event.genre,
              dialog = GenreManagementDialog.Rename(event.genre),
              apiState = GenreManagementApiState.Idle,
            )
          }
        }
      is GenreManagementEvent.UpdateRenameDraft ->
        _uiState.update { it.copy(renameDraft = event.value) }
      is GenreManagementEvent.BeginDelete ->
        if (!_uiState.value.isMutating) {
          _uiState.update { it.copy(dialog = GenreManagementDialog.Delete(event.genre)) }
        }
      GenreManagementEvent.DismissDialog ->
        _uiState.update { it.copy(dialog = null, editingGenre = null) }
      GenreManagementEvent.ConfirmRename -> rename()
      GenreManagementEvent.ConfirmDelete -> delete()
      GenreManagementEvent.ClearApiState ->
        _uiState.update { it.copy(apiState = GenreManagementApiState.Idle) }
    }
  }

  private fun load() {
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(state = GenericState.Loading, apiState = GenreManagementApiState.Loading)
      }
      repository.loadGenres().fold(
        onSuccess = { genres ->
          _uiState.update {
            it.copy(
              state = GenericState.Success,
              apiState = GenreManagementApiState.Idle,
              genres = sortedGenres(genres),
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              state = GenericState.Failure(error.message),
              apiState =
                GenreManagementApiState.Failure(
                  error.message,
                  error is MetadataAccessDeniedException,
                ),
            )
          }
        },
      )
    }
  }

  private fun rename() {
    val current = _uiState.value.editingGenre ?: return
    val target = _uiState.value.renameDraft.trim()
    if (target.isBlank() || _uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          dialog = null,
          apiState = GenreManagementApiState.Mutating(GenreOperation.Rename),
        )
      }
      repository.renameGenre(current, target).fold(
        onSuccess = { result ->
          _uiState.update {
            it.copy(
              state = GenericState.Success,
              apiState =
                GenreManagementApiState.RenameSuccess(result.updatedItemCount, result.merged),
              editingGenre = null,
            )
          }
          reloadAfterMutation()
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              apiState =
                GenreManagementApiState.Failure(
                  error.message,
                  error is MetadataAccessDeniedException,
                  operation = GenreOperation.Rename,
                ),
            )
          }
        },
      )
    }
  }

  private fun delete() {
    val genre = (_uiState.value.dialog as? GenreManagementDialog.Delete)?.genre ?: return
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          dialog = null,
          apiState = GenreManagementApiState.Mutating(GenreOperation.Delete),
        )
      }
      repository.deleteGenre(genre).fold(
        onSuccess = { result ->
          _uiState.update {
            it.copy(
              state = GenericState.Success,
              apiState = GenreManagementApiState.DeleteSuccess(result.updatedItemCount),
            )
          }
          reloadAfterMutation()
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              apiState =
                GenreManagementApiState.Failure(
                  error.message,
                  error is MetadataAccessDeniedException,
                  operation = GenreOperation.Delete,
                ),
            )
          }
        },
      )
    }
  }

  private suspend fun reloadAfterMutation() {
    repository.loadGenres().onSuccess { genres ->
      _uiState.update { it.copy(genres = sortedGenres(genres)) }
    }
  }
}

sealed interface GenreManagementEvent {
  data object Load : GenreManagementEvent
  data object Retry : GenreManagementEvent
  data class BeginRename(val genre: String) : GenreManagementEvent
  data class UpdateRenameDraft(val value: String) : GenreManagementEvent
  data class BeginDelete(val genre: String) : GenreManagementEvent
  data object DismissDialog : GenreManagementEvent
  data object ConfirmRename : GenreManagementEvent
  data object ConfirmDelete : GenreManagementEvent
  data object ClearApiState : GenreManagementEvent
}
