package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import javax.inject.Inject

class PlayerRepository @Inject constructor(private val libraryItemRepo: LibraryItemRepo) {
  fun item(id: String): PlayerUiState {
    return PlayerUiState(id, state = PlayerState.Small)
  }
}

enum class PlayerState {
  Hidden,
  TempHidden,
  Big,
  Small,
}

data class PlayerUiState(val id: String = "", val state: PlayerState = PlayerState.Hidden)
