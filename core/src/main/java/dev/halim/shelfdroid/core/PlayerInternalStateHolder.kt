package dev.halim.shelfdroid.core

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class PlayerInternalState(
  val mediaStructure: MediaStructure = MediaStructure.SingleTrack,
  val sessionId: String = "",
  val startOffset: Double = 0.0,
)

@Singleton
class PlayerInternalStateHolder @Inject constructor() {
  private val _internalState = MutableStateFlow(PlayerInternalState())

  fun changeMedia(uiState: PlayerUiState, sessionId: String) {
    val hasChapter = uiState.playerChapters.isNotEmpty()
    val multipleTrack = uiState.playerTracks.size > 1
    val mediaStructure = MediaStructure.from(hasChapter, multipleTrack)
    val startOffset = uiState.currentChapter?.startTimeSeconds ?: uiState.currentTrack.startOffset
    _internalState.update {
      it.copy(mediaStructure = mediaStructure, sessionId = sessionId, startOffset = startOffset)
    }
  }

  fun setStartOffset(startOffset: Double) {
    _internalState.update { it.copy(startOffset = startOffset) }
  }

  fun sessionId() = _internalState.value.sessionId

  fun startOffset() = _internalState.value.startOffset
}
