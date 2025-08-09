package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.PlayerUiState
import javax.inject.Inject

class PlayerFinder @Inject constructor() {
  // Return chapter duration if exist, else book duration
  fun chapterOrBookDuration(uiState: PlayerUiState): Double {
    val currentChapter = uiState.currentChapter
    val currentTrack = uiState.currentTrack
    val duration =
      if (currentChapter != null) {
        (currentChapter.endTimeSeconds - currentChapter.startTimeSeconds)
      } else {
        currentTrack.duration
      }

    return duration
  }

  // Find chapter position (in second) based on raw position (in ms, from ExoPlayer)
  fun chapterOrBookPosition(uiState: PlayerUiState, rawPositionMs: Long): Double {
    val currentChapter = uiState.currentChapter
    val currentTrack = uiState.currentTrack
    val position =
      if (currentChapter != null) {
        if (uiState.playerTracks.size > 1) {
          ((rawPositionMs / 1000) + currentTrack.startOffset).toFloat()
        } else {
          (rawPositionMs / 1000).toFloat()
        }
      } else {
        ((rawPositionMs / 1000) - currentTrack.startOffset).toFloat()
      }
    return position.toDouble()
  }

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

  fun startTime(uiState: PlayerUiState): Double {
    val currentChapter = uiState.currentChapter
    val currentTrack = uiState.currentTrack
    val startTime = currentChapter?.startTimeSeconds ?: currentTrack.startOffset
    return startTime
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
}
