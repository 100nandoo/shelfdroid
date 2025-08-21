package dev.halim.shelfdroid.core

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class PlayerInternalState(
  val mediaStructure: MediaStructure = MediaStructure.SingleTrack,
  val sessionId: String = "",
  val startOffset: Double = 0.0,
  val duration: Double = 0.0,
  val position: Double = 0.0,
  val isBook: Boolean = true,
)

@Singleton
class PlayerInternalStateHolder @Inject constructor() {
  private val _internalState = MutableStateFlow(PlayerInternalState())

  fun changeMedia(uiState: PlayerUiState, sessionId: String) {
    val hasChapter = uiState.playerChapters.isNotEmpty()
    val multipleTrack = uiState.playerTracks.size > 1
    val mediaStructure = MediaStructure.from(hasChapter, multipleTrack)
    val startOffset = uiState.currentChapter?.startTimeSeconds ?: uiState.currentTrack.startOffset
    val duration =
      uiState.currentChapter?.endTimeSeconds?.minus(uiState.currentChapter.startTimeSeconds)
        ?: uiState.currentTrack.duration
    _internalState.update {
      it.copy(
        mediaStructure = mediaStructure,
        sessionId = sessionId,
        startOffset = startOffset,
        duration = duration,
        isBook = uiState.episodeId.isBlank(),
      )
    }
  }

  fun changeChapter(chapter: PlayerChapter) {
    val duration = chapter.endTimeSeconds - chapter.startTimeSeconds
    _internalState.update { it.copy(startOffset = chapter.startTimeSeconds, duration = duration) }
  }

  fun changePosition(rawPositionMs: Double): Double {
    val rawPositionSeconds = rawPositionMs / 1000
    val position =
      when (_internalState.value.mediaStructure) {
        MediaStructure.MultiTrackWithChapters -> (rawPositionSeconds + startOffset())
        MediaStructure.SingleTrackWithChapters -> rawPositionSeconds
        else -> (rawPositionSeconds - _internalState.value.startOffset)
      }
    _internalState.update { it.copy(position = position) }
    return position
  }

  fun mediaStructure() = _internalState.value.mediaStructure

  fun sessionId() = _internalState.value.sessionId

  fun startOffset() = _internalState.value.startOffset

  fun duration() = _internalState.value.duration

  fun isBook() = _internalState.value.isBook
}
