package dev.halim.shelfdroid.core.ui.screen.metadata.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilsContract
import dev.halim.shelfdroid.core.data.metadata.genre.GenreApiState
import dev.halim.shelfdroid.core.data.metadata.genre.GenreDialog
import dev.halim.shelfdroid.core.data.metadata.genre.GenreOperation
import dev.halim.shelfdroid.core.data.metadata.genre.GenreUiState
import dev.halim.shelfdroid.core.data.metadata.genre.sortedGenres
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GenreViewModel @Inject constructor(private val repository: MetadataUtilsContract) :
  ViewModel() {
  private val _uiState = MutableStateFlow(GenreUiState())
  val uiState: StateFlow<GenreUiState> =
    _uiState
      .onStart { load() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenreUiState())

  fun onEvent(event: GenreEvent) {
    when (event) {
      GenreEvent.Load,
      GenreEvent.Retry -> load()
      is GenreEvent.BeginRename ->
        if (!_uiState.value.isMutating) {
          _uiState.update {
            it.copy(
              editingGenre = event.genre,
              renameDraft = event.genre,
              dialog = GenreDialog.Rename(event.genre),
              apiState = GenreApiState.Idle,
            )
          }
        }
      is GenreEvent.UpdateRenameDraft -> _uiState.update { it.copy(renameDraft = event.value) }
      is GenreEvent.BeginDelete ->
        if (!_uiState.value.isMutating) {
          _uiState.update { it.copy(dialog = GenreDialog.Delete(event.genre)) }
        }
      GenreEvent.DismissDialog -> _uiState.update { it.copy(dialog = null, editingGenre = null) }
      GenreEvent.ConfirmRename -> rename()
      GenreEvent.ConfirmDelete -> delete()
      GenreEvent.ClearApiState -> _uiState.update { it.copy(apiState = GenreApiState.Idle) }
    }
  }

  private fun load() {
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(state = GenericState.Loading, apiState = GenreApiState.Loading)
      }
      repository
        .loadGenres()
        .fold(
          onSuccess = { genres ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = GenreApiState.Idle,
                genres = sortedGenres(genres),
              )
            }
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                state = GenericState.Failure(error.message),
                apiState = GenreApiState.Failure(error.message),
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
          apiState = GenreApiState.Mutating(GenreOperation.Rename),
        )
      }
      repository
        .renameGenre(current, target)
        .fold(
          onSuccess = { result ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = GenreApiState.RenameSuccess(result.updatedItemCount, result.merged),
                editingGenre = null,
              )
            }
            reloadAfterMutation()
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                apiState =
                  GenreApiState.Failure(
                    error.message,
                    operation = GenreOperation.Rename,
                  )
              )
            }
          },
        )
    }
  }

  private fun delete() {
    val genre = (_uiState.value.dialog as? GenreDialog.Delete)?.genre ?: return
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          dialog = null,
          apiState = GenreApiState.Mutating(GenreOperation.Delete),
        )
      }
      repository
        .deleteGenre(genre)
        .fold(
          onSuccess = { result ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = GenreApiState.DeleteSuccess(result.updatedItemCount),
              )
            }
            reloadAfterMutation()
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                apiState =
                  GenreApiState.Failure(
                    error.message,
                    operation = GenreOperation.Delete,
                  )
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

sealed interface GenreEvent {
  data object Load : GenreEvent

  data object Retry : GenreEvent

  data class BeginRename(val genre: String) : GenreEvent

  data class UpdateRenameDraft(val value: String) : GenreEvent

  data class BeginDelete(val genre: String) : GenreEvent

  data object DismissDialog : GenreEvent

  data object ConfirmRename : GenreEvent

  data object ConfirmDelete : GenreEvent

  data object ClearApiState : GenreEvent
}
