package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerTrack
import dev.halim.shelfdroid.core.PlayerUiState
import javax.inject.Inject

class PlayerFinder @Inject constructor() {
  /**
   * Current book position (in ms) since the beginning of the book
   *
   * Usage: For seekTo
   */
  fun bookRawPositionMs(uiState: PlayerUiState, target: Float, chapterDurationMs: Long): Long {
    val isBook = uiState.episodeId.isBlank()
    val currentChapter = uiState.currentChapter
    val currentTrack = uiState.currentTrack
    val positionMs =
      if (isBook) {
        if (currentChapter != null) {
          val currentTrackOffsetMs = currentTrack.startOffset * 1000
          if (uiState.playerTracks.size > 1) {
            (target * chapterDurationMs - currentTrackOffsetMs).toLong()
          } else {
            (target * chapterDurationMs).toLong()
          }
        } else {
          (target * chapterDurationMs).toLong()
        }
      } else {
        (target * chapterDurationMs).toLong()
      }
    return positionMs
  }

  /**
   * Current book position (in second) since the beginning of the book
   *
   * Usage: For sync progress to server
   */
  fun bookPosition(startOffset: Double, rawPositionMs: Long): Double {
    val position = (rawPositionMs / 1000) + startOffset
    return position
  }

  // Find chapter closest to currentTime
  fun playerChapter(playerChapters: List<PlayerChapter>, currentTime: Double): PlayerChapter? {
    if (playerChapters.isEmpty()) return null
    return playerChapters
      .sortedBy { it.startTimeSeconds }
      .lastOrNull { it.startTimeSeconds <= currentTime } ?: playerChapters.first()
  }

  fun trackFromChapter(tracks: List<PlayerTrack>, target: Double): PlayerTrack {
    return if (tracks.size == 1) {
      return tracks.first()
    } else {
      val track = tracks.lastOrNull { it.startOffset <= target } ?: tracks.first()
      track
    }
  }

  fun trackFromCurrentTime(uiState: PlayerUiState, currentTime: Double): PlayerTrack {
    val tracks = uiState.playerTracks
    return if (tracks.size == 1) {
      return tracks.first()
    } else {
      val track = tracks.lastOrNull { it.startOffset <= currentTime } ?: tracks.first()
      track
    }
  }
}
