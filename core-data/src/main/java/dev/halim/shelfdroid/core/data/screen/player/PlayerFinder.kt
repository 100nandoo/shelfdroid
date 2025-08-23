package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerTrack
import dev.halim.shelfdroid.core.PlayerUiState
import javax.inject.Inject

class PlayerFinder @Inject constructor() {
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

  fun tracksFromChapter(tracks: List<PlayerTrack>, chapter: PlayerChapter): List<PlayerTrack> {
    return if (tracks.size == 1) {
      tracks
    } else {
      tracks.filter { track ->
        val trackStart = track.startOffset
        val trackEnd = track.startOffset + track.duration
        trackEnd > chapter.startTimeSeconds && trackStart < chapter.endTimeSeconds
      }
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
