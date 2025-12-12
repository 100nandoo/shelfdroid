package dev.halim.shelfdroid.media.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.Lazy
import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerEventListener
@Inject
constructor(private val player: Lazy<ExoPlayer>, private val podcastRepository: PodcastRepository) {
  @OptIn(UnstableApi::class)
  fun listen(
    uiState: PlayerUiState,
    changeChapterEvent: () -> Unit,
    onEvents: Player.(Player.Events) -> Unit,
  ): Job {
    return CoroutineScope(Dispatchers.Default).launch {
      player.get().apply {
        listen { event ->
          goToNextChapterLogic(this@apply, uiState, changeChapterEvent)
          onEvents(event)
        }
      }
    }
  }

  private fun goToNextChapterLogic(
    player: ExoPlayer,
    uiState: PlayerUiState,
    changeChapterEvent: () -> Unit,
  ) {
    player.apply {
      val isEnded = this.playbackState == Player.STATE_ENDED
      if (isEnded) {
        if (uiState.episodeId.isBlank()) {
          val start = uiState.currentChapter?.startTimeSeconds ?: 0.0
          val end = uiState.currentChapter?.endTimeSeconds?.minus(start)?.toLong() ?: 0
          val isReachEndChapter = (currentPosition / 1000) >= end
          val isNotLastChapter = uiState.currentChapter?.chapterPosition != ChapterPosition.Last
          if (isReachEndChapter && isNotLastChapter) {
            changeChapterEvent()
          }
        } else {
          CoroutineScope(Dispatchers.IO).launch {
            podcastRepository.markIsFinished(uiState.id, uiState.episodeId)
          }
        }
      }
    }
  }
}
