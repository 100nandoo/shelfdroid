package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.GenericState

data class TagManagementUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: TagManagementApiState = TagManagementApiState.Idle,
  val tags: List<String> = emptyList(),
  val editingTag: String? = null,
  val renameDraft: String = "",
  val dialog: TagManagementDialog? = null,
) {
  val isMutating: Boolean
    get() = apiState is TagManagementApiState.Mutating
}

sealed interface TagManagementDialog {
  data class Rename(val tag: String) : TagManagementDialog
  data class Delete(val tag: String) : TagManagementDialog
}

sealed interface TagManagementApiState {
  data object Idle : TagManagementApiState
  data object Loading : TagManagementApiState
  data class Mutating(val operation: Operation) : TagManagementApiState
  data class RenameSuccess(val updatedItemCount: Int, val merged: Boolean) : TagManagementApiState
  data class DeleteSuccess(val updatedItemCount: Int) : TagManagementApiState
  data class Failure(val message: String?, val accessDenied: Boolean = false) : TagManagementApiState
}

enum class Operation { Rename, Delete }

data class TagRenameCollision(val exact: Boolean, val caseOnly: Boolean)

data class TagMutation(val updatedItemCount: Int, val merged: Boolean = false)

fun sortedTags(tags: Iterable<String>): List<String> =
  tags.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

fun tagRenameCollision(currentTag: String, newTag: String, tags: Iterable<String>): TagRenameCollision {
  val current = currentTag.trim()
  val target = newTag.trim()
  val exact = tags.any { it != current && it == target }
  val caseOnly =
    !exact &&
      tags.any { it != current && it.equals(target, ignoreCase = true) }
  return TagRenameCollision(exact = exact, caseOnly = caseOnly)
}
