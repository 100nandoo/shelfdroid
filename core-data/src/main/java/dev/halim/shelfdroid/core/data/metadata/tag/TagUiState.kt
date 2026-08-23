package dev.halim.shelfdroid.core.data.metadata.tag

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataMutation
import dev.halim.shelfdroid.core.data.metadata.MetadataRenameCollision
import dev.halim.shelfdroid.core.data.metadata.metadataRenameCollision
import dev.halim.shelfdroid.core.data.metadata.sortedMetadataItems

data class TagUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: TagApiState = TagApiState.Idle,
  val tags: List<String> = emptyList(),
  val editingTag: String? = null,
  val renameDraft: String = "",
  val dialog: TagDialog? = null,
) {
  val isMutating: Boolean
    get() = apiState is TagApiState.Mutating
}

sealed interface TagDialog {
  data class Rename(val tag: String) : TagDialog

  data class Delete(val tag: String) : TagDialog
}

sealed interface TagApiState {
  data object Idle : TagApiState

  data object Loading : TagApiState

  data class Mutating(val operation: Operation) : TagApiState

  data class RenameSuccess(val updatedItemCount: Int, val merged: Boolean) : TagApiState

  data class DeleteSuccess(val updatedItemCount: Int) : TagApiState

  data class Failure(val message: String?) : TagApiState
}

enum class Operation {
  Rename,
  Delete,
}

typealias TagMutation = MetadataMutation

typealias TagRenameCollision = MetadataRenameCollision

fun sortedTags(tags: Iterable<String>): List<String> = sortedMetadataItems(tags)

fun tagRenameCollision(
  currentTag: String,
  newTag: String,
  tags: Iterable<String>,
): TagRenameCollision = metadataRenameCollision(currentTag, newTag, tags)
