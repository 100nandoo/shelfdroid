package dev.halim.shelfdroid.core.data.metadata

import dev.halim.shelfdroid.core.data.GenericState

data class GenreManagementUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: GenreManagementApiState = GenreManagementApiState.Idle,
  val genres: List<String> = emptyList(),
  val editingGenre: String? = null,
  val renameDraft: String = "",
  val dialog: GenreManagementDialog? = null,
) {
  val isMutating: Boolean
    get() = apiState is GenreManagementApiState.Mutating
}

sealed interface GenreManagementDialog {
  data class Rename(val genre: String) : GenreManagementDialog
  data class Delete(val genre: String) : GenreManagementDialog
}

sealed interface GenreManagementApiState {
  data object Idle : GenreManagementApiState
  data object Loading : GenreManagementApiState
  data class Mutating(val operation: GenreOperation) : GenreManagementApiState
  data class RenameSuccess(val updatedItemCount: Int, val merged: Boolean) : GenreManagementApiState
  data class DeleteSuccess(val updatedItemCount: Int) : GenreManagementApiState
  data class Failure(
    val message: String?,
    val accessDenied: Boolean = false,
    val operation: GenreOperation? = null,
  ) : GenreManagementApiState
}

enum class GenreOperation { Rename, Delete }

data class GenreRenameCollision(val exact: Boolean, val caseOnly: Boolean)

data class GenreMutation(val updatedItemCount: Int, val merged: Boolean = false)

fun sortedGenres(genres: Iterable<String>): List<String> =
  genres.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

fun genreRenameCollision(
  currentGenre: String,
  newGenre: String,
  genres: Iterable<String>,
): GenreRenameCollision {
  val current = currentGenre.trim()
  val target = newGenre.trim()
  val exact = genres.any { it != current && it == target }
  val caseOnly =
    !exact && genres.any { it != current && it.equals(target, ignoreCase = true) }
  return GenreRenameCollision(exact = exact, caseOnly = caseOnly)
}
