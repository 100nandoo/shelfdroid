package dev.halim.shelfdroid.core.ui.screen.metadata.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilsContract
import dev.halim.shelfdroid.core.data.metadata.Operation
import dev.halim.shelfdroid.core.data.metadata.TagApiState
import dev.halim.shelfdroid.core.data.metadata.TagDialog
import dev.halim.shelfdroid.core.data.metadata.TagUiState
import dev.halim.shelfdroid.core.data.metadata.sortedTags
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TagViewModel @Inject constructor(private val repository: MetadataUtilsContract) :
  ViewModel() {
  private val _uiState = MutableStateFlow(TagUiState())
  val uiState: StateFlow<TagUiState> =
    _uiState
      .onStart { load() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagUiState())

  fun onEvent(event: TagEvent) {
    when (event) {
      TagEvent.Load -> load()
      TagEvent.Retry -> load()
      is TagEvent.BeginRename ->
        if (!_uiState.value.isMutating) {
          _uiState.update {
            it.copy(
              editingTag = event.tag,
              renameDraft = event.tag,
              dialog = TagDialog.Rename(event.tag),
              apiState = TagApiState.Idle,
            )
          }
        }
      is TagEvent.UpdateRenameDraft -> _uiState.update { it.copy(renameDraft = event.value) }
      is TagEvent.BeginDelete ->
        if (!_uiState.value.isMutating) {
          _uiState.update { it.copy(dialog = TagDialog.Delete(event.tag)) }
        }
      TagEvent.DismissDialog -> _uiState.update { it.copy(dialog = null, editingTag = null) }
      TagEvent.ConfirmRename -> rename()
      TagEvent.ConfirmDelete -> delete()
      TagEvent.ClearApiState -> _uiState.update { it.copy(apiState = TagApiState.Idle) }
    }
  }

  private fun load() {
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update { it.copy(state = GenericState.Loading, apiState = TagApiState.Loading) }
      repository
        .loadTags()
        .fold(
          onSuccess = { tags ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = TagApiState.Idle,
                tags = sortedTags(tags),
              )
            }
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                state = GenericState.Failure(error.message),
                apiState =
                  TagApiState.Failure(error.message, error is MetadataAccessDeniedException),
              )
            }
          },
        )
    }
  }

  private fun rename() {
    val current = _uiState.value.editingTag ?: return
    val target = _uiState.value.renameDraft.trim()
    if (target.isBlank() || _uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update { it.copy(dialog = null, apiState = TagApiState.Mutating(Operation.Rename)) }
      repository
        .renameTag(current, target)
        .fold(
          onSuccess = { result ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = TagApiState.RenameSuccess(result.updatedItemCount, result.merged),
                editingTag = null,
              )
            }
            reloadAfterMutation()
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                apiState =
                  TagApiState.Failure(error.message, error is MetadataAccessDeniedException)
              )
            }
          },
        )
    }
  }

  private fun delete() {
    val tag = (_uiState.value.dialog as? TagDialog.Delete)?.tag ?: return
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update { it.copy(dialog = null, apiState = TagApiState.Mutating(Operation.Delete)) }
      repository
        .deleteTag(tag)
        .fold(
          onSuccess = { result ->
            _uiState.update {
              it.copy(
                state = GenericState.Success,
                apiState = TagApiState.DeleteSuccess(result.updatedItemCount),
              )
            }
            reloadAfterMutation()
          },
          onFailure = { error ->
            _uiState.update {
              it.copy(
                apiState =
                  TagApiState.Failure(error.message, error is MetadataAccessDeniedException)
              )
            }
          },
        )
    }
  }

  private suspend fun reloadAfterMutation() {
    repository.loadTags().onSuccess { tags -> _uiState.update { it.copy(tags = sortedTags(tags)) } }
  }
}

sealed interface TagEvent {
  data object Load : TagEvent

  data object Retry : TagEvent

  data class BeginRename(val tag: String) : TagEvent

  data class UpdateRenameDraft(val value: String) : TagEvent

  data class BeginDelete(val tag: String) : TagEvent

  data object DismissDialog : TagEvent

  data object ConfirmRename : TagEvent

  data object ConfirmDelete : TagEvent

  data object ClearApiState : TagEvent
}
