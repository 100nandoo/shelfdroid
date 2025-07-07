package dev.halim.shelfdroid.core.ui.media3

import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.exoplayer.ExoPlayer
import dev.halim.shelfdroid.core.data.screen.player.ChapterPosition
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerEventListener @Inject constructor() {
  fun listen(player: ExoPlayer, uiState: PlayerUiState, changeChapterEvent: () -> Unit): Job {
    return CoroutineScope(Dispatchers.Default).launch {
      player.apply {
        listen { event -> changeChapterLogic(this@apply, uiState, changeChapterEvent) }
      }
    }
  }

  private fun changeChapterLogic(
    player: ExoPlayer,
    uiState: PlayerUiState,
    changeChapterEvent: () -> Unit,
  ) {
    player.apply {
      if (this.playbackState == Player.STATE_ENDED && uiState.episodeId.isBlank()) {
        val start = uiState.currentChapter?.startTimeSeconds ?: 0.0
        val end = uiState.currentChapter?.endTimeSeconds?.minus(start)?.toLong()
        val isReachEndChapter = end == currentPosition / 1000
        val isNotLastChapter = uiState.currentChapter?.chapterPosition != ChapterPosition.Last
        if (isReachEndChapter && isNotLastChapter) {
          changeChapterEvent()
        }
      }
    }
  }
}
