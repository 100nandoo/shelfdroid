package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import javax.inject.Inject

class PlayerRepository
@Inject
constructor(private val libraryItemRepo: LibraryItemRepo, private val progressRepo: ProgressRepo) {
  fun item(id: String): PlayerUiState {
    val result = libraryItemRepo.byId(id)
    val progress = progressRepo.byLibraryItemId(id)
    return if (result != null) {
      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        author = result.author,
        title = result.title,
        cover = result.cover,
        progress = progress?.progress?.toFloat() ?: 0f,
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }
}

sealed class PlayerState {
  class Hidden(error: Error? = null) : PlayerState()

  data object TempHidden : PlayerState()

  data object Big : PlayerState()

  data object Small : PlayerState()
}

data class PlayerUiState(
  val state: PlayerState = PlayerState.Hidden(),
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val progress: Float = 0f,
)
