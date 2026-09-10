package dev.halim.shelfdroid.media.service

import dev.halim.shelfdroid.core.PlayerUiState

data class NextChapterControlState(
  val visible: Boolean,
  val enabled: Boolean,
)

fun nextChapterControlState(
  uiState: PlayerUiState,
  isTransitioning: Boolean = false,
): NextChapterControlState {
  val isMultiChapterBook =
    uiState.episodeId.isBlank() && uiState.currentChapter != null && uiState.playerChapters.size > 1
  if (!isMultiChapterBook) {
    return NextChapterControlState(visible = false, enabled = false)
  }

  val currentChapterIndex = uiState.playerChapters.indexOf(uiState.currentChapter)
  val hasNextChapter = currentChapterIndex in 0 until uiState.playerChapters.lastIndex
  return NextChapterControlState(
    visible = true,
    enabled = hasNextChapter && !isTransitioning,
  )
}
