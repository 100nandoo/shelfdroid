package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.GenericState

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

  data class Failure(val message: String?, val accessDenied: Boolean = false) : TagApiState
}

enum class Operation {
  Rename,
  Delete,
}

data class TagRenameCollision(val exact: Boolean, val caseOnly: Boolean)

data class TagMutation(val updatedItemCount: Int, val merged: Boolean = false)

fun sortedTags(tags: Iterable<String>): List<String> =
  tags.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

fun tagRenameCollision(
  currentTag: String,
  newTag: String,
  tags: Iterable<String>,
): TagRenameCollision {
  val current = currentTag.trim()
  val target = newTag.trim()
  val exact = tags.any { it != current && it == target }
  val caseOnly = !exact && tags.any { it != current && it.equals(target, ignoreCase = true) }
  return TagRenameCollision(exact = exact, caseOnly = caseOnly)
}
