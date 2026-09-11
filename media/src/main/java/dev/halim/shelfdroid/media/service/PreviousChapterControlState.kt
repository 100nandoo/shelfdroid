package dev.halim.shelfdroid.media.service

import dev.halim.shelfdroid.core.PlayerUiState

data class PreviousChapterControlState(
  val visible: Boolean,
  val enabled: Boolean,
)

fun previousChapterControlState(
  uiState: PlayerUiState,
  isTransitioning: Boolean = false,
): PreviousChapterControlState {
  val isMultiChapterBook =
    uiState.episodeId.isBlank() && uiState.currentChapter != null && uiState.playerChapters.size > 1
  if (!isMultiChapterBook) {
    return PreviousChapterControlState(visible = false, enabled = false)
  }

  val currentChapterIndex = uiState.playerChapters.indexOf(uiState.currentChapter)
  return PreviousChapterControlState(
    visible = true,
    enabled = currentChapterIndex > 0 && !isTransitioning,
  )
}
