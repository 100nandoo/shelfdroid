package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.PlayerUiState
import javax.inject.Inject

class PlayerFinder @Inject constructor() {
  fun bookDuration(uiState: PlayerUiState): Double {
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
  fun bookChapterPosition(uiState: PlayerUiState, rawPositionMs: Long): Double {
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

  fun bookPosition(uiState: PlayerUiState, rawPositionMs: Long): Long {
    val currentChapter = uiState.currentChapter
    val currentTrack = uiState.currentTrack
    val position =
      if (currentChapter != null) {
        if (uiState.playerTracks.size > 1) {
          (rawPositionMs / 1000) + currentTrack.startOffset
        } else {
          (rawPositionMs / 1000) + currentChapter.startTimeSeconds
        }
      } else {
        (rawPositionMs / 1000) + currentTrack.startOffset
      }
    return position.toLong()
  }
}
