package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract
import dev.halim.shelfdroid.core.data.metadata.Operation
import dev.halim.shelfdroid.core.data.metadata.TagManagementApiState
import dev.halim.shelfdroid.core.data.metadata.TagManagementDialog
import dev.halim.shelfdroid.core.data.metadata.TagManagementUiState
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
class TagManagementViewModel
@Inject
constructor(private val repository: MetadataUtilitiesRepositoryContract) : ViewModel() {
  private val _uiState = MutableStateFlow(TagManagementUiState())
  val uiState: StateFlow<TagManagementUiState> =
    _uiState
      .onStart { load() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagManagementUiState())

  fun onEvent(event: TagManagementEvent) {
    when (event) {
      TagManagementEvent.Load -> load()
      TagManagementEvent.Retry -> load()
      is TagManagementEvent.BeginRename ->
        if (!_uiState.value.isMutating) {
          _uiState.update {
            it.copy(
              editingTag = event.tag,
              renameDraft = event.tag,
              dialog = TagManagementDialog.Rename(event.tag),
              apiState = TagManagementApiState.Idle,
            )
          }
        }
      is TagManagementEvent.UpdateRenameDraft -> _uiState.update { it.copy(renameDraft = event.value) }
      is TagManagementEvent.BeginDelete ->
        if (!_uiState.value.isMutating) {
          _uiState.update { it.copy(dialog = TagManagementDialog.Delete(event.tag)) }
        }
      TagManagementEvent.DismissDialog -> _uiState.update { it.copy(dialog = null, editingTag = null) }
      TagManagementEvent.ConfirmRename -> rename()
      TagManagementEvent.ConfirmDelete -> delete()
      TagManagementEvent.ClearApiState -> _uiState.update { it.copy(apiState = TagManagementApiState.Idle) }
    }
  }

  private fun load() {
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update { it.copy(state = GenericState.Loading, apiState = TagManagementApiState.Loading) }
      repository.loadTags().fold(
        onSuccess = { tags ->
          _uiState.update {
            it.copy(state = GenericState.Success, apiState = TagManagementApiState.Idle, tags = sortedTags(tags))
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              state = GenericState.Failure(error.message),
              apiState = TagManagementApiState.Failure(error.message, error is MetadataAccessDeniedException),
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
      _uiState.update { it.copy(dialog = null, apiState = TagManagementApiState.Mutating(Operation.Rename)) }
      repository.renameTag(current, target).fold(
        onSuccess = { result ->
          _uiState.update {
            it.copy(
              state = GenericState.Success,
              apiState = TagManagementApiState.RenameSuccess(result.updatedItemCount, result.merged),
              editingTag = null,
            )
          }
          reloadAfterMutation()
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(apiState = TagManagementApiState.Failure(error.message, error is MetadataAccessDeniedException))
          }
        },
      )
    }
  }

  private fun delete() {
    val tag = (_uiState.value.dialog as? TagManagementDialog.Delete)?.tag ?: return
    if (_uiState.value.isMutating) return
    viewModelScope.launch {
      _uiState.update { it.copy(dialog = null, apiState = TagManagementApiState.Mutating(Operation.Delete)) }
      repository.deleteTag(tag).fold(
        onSuccess = { result ->
          _uiState.update {
            it.copy(state = GenericState.Success, apiState = TagManagementApiState.DeleteSuccess(result.updatedItemCount))
          }
          reloadAfterMutation()
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(apiState = TagManagementApiState.Failure(error.message, error is MetadataAccessDeniedException))
          }
        },
      )
    }
  }

  private suspend fun reloadAfterMutation() {
    repository.loadTags().onSuccess { tags -> _uiState.update { it.copy(tags = sortedTags(tags)) } }
  }
}

sealed interface TagManagementEvent {
  data object Load : TagManagementEvent
  data object Retry : TagManagementEvent
  data class BeginRename(val tag: String) : TagManagementEvent
  data class UpdateRenameDraft(val value: String) : TagManagementEvent
  data class BeginDelete(val tag: String) : TagManagementEvent
  data object DismissDialog : TagManagementEvent
  data object ConfirmRename : TagManagementEvent
  data object ConfirmDelete : TagManagementEvent
  data object ClearApiState : TagManagementEvent
}
