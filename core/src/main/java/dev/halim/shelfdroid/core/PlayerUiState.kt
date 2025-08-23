package dev.halim.shelfdroid.core

import kotlin.time.Duration

sealed class PlayerState {
  class Hidden(error: Error? = null) : PlayerState()

  data object TempHidden : PlayerState()

  data object Big : PlayerState()

  data object Small : PlayerState()
}

sealed class ExoState {
  data object Playing : ExoState()

  data object Pause : ExoState()
}

sealed class MediaStructure {
  data object SingleTrackWithChapters : MediaStructure()

  data object MultiTrackWithChapters : MediaStructure()

  data object SingleTrack : MediaStructure()

  data object MultiTrack : MediaStructure()

  fun isSingleTrack() = this is SingleTrack || this is SingleTrackWithChapters

  companion object {
    fun from(hasChapter: Boolean, multipleTrack: Boolean): MediaStructure {
      return when {
        hasChapter && multipleTrack -> MultiTrackWithChapters
        hasChapter && !multipleTrack -> SingleTrackWithChapters
        !hasChapter && multipleTrack -> MultiTrack
        else -> SingleTrack
      }
    }
  }
}

data class PlayerUiState(
  val state: PlayerState = PlayerState.Hidden(),
  val exoState: ExoState = ExoState.Pause,
  val multipleButtonState: MultipleButtonState = MultipleButtonState(),
  val id: String = "",
  val episodeId: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val currentTime: Double = 0.0,
  val playerTracks: List<PlayerTrack> = emptyList(),
  val currentTrack: PlayerTrack = PlayerTrack(),
  val playerChapters: List<PlayerChapter> = emptyList(),
  val currentChapter: PlayerChapter? = PlayerChapter(),
  val playbackProgress: PlaybackProgress = PlaybackProgress(),
  val advancedControl: AdvancedControl = AdvancedControl(),
  val playerBookmarks: List<PlayerBookmark> = emptyList(),
  val newBookmarkTime: PlayerBookmark = PlayerBookmark(),
  val download: DownloadUiState = DownloadUiState(),
)

data class PlayerTrack(
  val index: Int = 0,
  val url: String = "",
  val duration: Double = 0.0,
  val startOffset: Double = 0.0,
)

data class PlayerChapter(
  val id: Int = 0,
  val startTimeSeconds: Double = 0.0,
  val endTimeSeconds: Double = 0.0,
  val startFormattedTime: String = "",
  val endFormattedTime: String = "",
  val title: String = "",
  val chapterPosition: ChapterPosition = ChapterPosition.First,
) {
  fun isFirst() = chapterPosition == ChapterPosition.First

  fun isLast() = chapterPosition == ChapterPosition.Last
}

enum class ChapterPosition {
  First,
  Last,
  Middle,
}

data class RawPlaybackProgress(val positionMs: Long = 0, val bufferedPosition: Long = 0)

data class PlaybackProgress(
  val position: Long = 0,
  val duration: Long = 0,
  val bufferedPosition: Float = 0f,
  val progress: Float = 0f,
)

data class AdvancedControl(
  val speed: Float = 1f,
  val sleepTimerLeft: Duration = Duration.Companion.ZERO,
)

data class PlayerBookmark(
  val title: String = "",
  val readableTime: String = "",
  val time: Long = 0,
)

data class MultipleButtonState(
  val seekBackEnabled: Boolean = false,
  val seekForwardEnabled: Boolean = false,
  val playPauseEnabled: Boolean = false,
  val showPlay: Boolean = true,
  val seekSliderEnabled: Boolean = false,
)
