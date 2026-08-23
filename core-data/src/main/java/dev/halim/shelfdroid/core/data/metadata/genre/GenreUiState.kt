package dev.halim.shelfdroid.core.data.metadata.genre

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataMutation
import dev.halim.shelfdroid.core.data.metadata.MetadataRenameCollision
import dev.halim.shelfdroid.core.data.metadata.metadataRenameCollision
import dev.halim.shelfdroid.core.data.metadata.sortedMetadataItems

data class GenreUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: GenreApiState = GenreApiState.Idle,
  val genres: List<String> = emptyList(),
  val editingGenre: String? = null,
  val renameDraft: String = "",
  val dialog: GenreDialog? = null,
) {
  val isMutating: Boolean
    get() = apiState is GenreApiState.Mutating
}

sealed interface GenreDialog {
  data class Rename(val genre: String) : GenreDialog

  data class Delete(val genre: String) : GenreDialog
}

sealed interface GenreApiState {
  data object Idle : GenreApiState

  data object Loading : GenreApiState

  data class Mutating(val operation: GenreOperation) : GenreApiState

  data class RenameSuccess(val updatedItemCount: Int, val merged: Boolean) : GenreApiState

  data class DeleteSuccess(val updatedItemCount: Int) : GenreApiState

  data class Failure(
    val message: String?,
    val operation: GenreOperation? = null,
  ) : GenreApiState
}

enum class GenreOperation {
  Rename,
  Delete,
}

typealias GenreMutation = MetadataMutation

typealias GenreRenameCollision = MetadataRenameCollision

fun sortedGenres(genres: Iterable<String>): List<String> = sortedMetadataItems(genres)

fun genreRenameCollision(
  currentGenre: String,
  newGenre: String,
  genres: Iterable<String>,
): GenreRenameCollision = metadataRenameCollision(currentGenre, newGenre, genres)
