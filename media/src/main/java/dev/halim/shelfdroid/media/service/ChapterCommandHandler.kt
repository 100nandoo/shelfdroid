package dev.halim.shelfdroid.media.service

import dev.halim.shelfdroid.core.PlayerUiState
import javax.inject.Inject
import javax.inject.Singleton

enum class ChapterCommand {
  Previous,
  Next,
}

data class ChapterCommandAvailability(
  val previousEnabled: Boolean,
  val nextEnabled: Boolean,
)

sealed interface ChapterCommandDecision {
  data object Restart : ChapterCommandDecision

  data class ChangeChapter(val targetIndex: Int) : ChapterCommandDecision

  data object Unavailable : ChapterCommandDecision
}

@Singleton
class ChapterCommandHandler @Inject constructor() {
  fun availability(uiState: PlayerUiState): ChapterCommandAvailability {
    val hasCurrentPlayback = uiState.id.isNotBlank()
    val currentIndex = uiState.playerChapters.indexOf(uiState.currentChapter)
    val hasNextChapter =
      uiState.episodeId.isBlank() &&
        uiState.playerChapters.size > 1 &&
        currentIndex in 0 until uiState.playerChapters.lastIndex

    return ChapterCommandAvailability(
      previousEnabled = hasCurrentPlayback,
      nextEnabled = hasCurrentPlayback && hasNextChapter,
    )
  }

  fun resolve(
    command: ChapterCommand,
    uiState: PlayerUiState,
    positionInPlayableUnitMs: Long,
  ): ChapterCommandDecision {
    val availability = availability(uiState)
    return when (command) {
      ChapterCommand.Previous -> {
        if (!availability.previousEnabled) {
          ChapterCommandDecision.Unavailable
        } else if (positionInPlayableUnitMs > PREVIOUS_RESTART_THRESHOLD_MS) {
          ChapterCommandDecision.Restart
        } else {
          val currentIndex = uiState.playerChapters.indexOf(uiState.currentChapter)
          if (currentIndex > 0) {
            ChapterCommandDecision.ChangeChapter(currentIndex - 1)
          } else {
            ChapterCommandDecision.Restart
          }
        }
      }
      ChapterCommand.Next -> {
        if (availability.nextEnabled) {
          val currentIndex = uiState.playerChapters.indexOf(uiState.currentChapter)
          ChapterCommandDecision.ChangeChapter(currentIndex + 1)
        } else {
          ChapterCommandDecision.Unavailable
        }
      }
    }
  }

  private companion object {
    const val PREVIOUS_RESTART_THRESHOLD_MS = 3_000L
  }
}
