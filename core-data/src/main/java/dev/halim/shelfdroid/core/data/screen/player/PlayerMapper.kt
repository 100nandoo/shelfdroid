package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class PlayerMapper
@Inject
constructor(private val helper: Helper, private val dataStoreManager: DataStoreManager) {

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.token.first() }

  suspend fun toPlayerTrack(audioTrack: AudioTrack): PlayerTrack {
    val url = helper.generateContentUrl(getToken(), audioTrack.contentUrl)
    return PlayerTrack(url, audioTrack.duration, audioTrack.startOffset)
  }

  fun toPlayerChapter(index: Int, bookChapter: BookChapter, totalChapters: Int): PlayerChapter {
    val position =
      when (index) {
        0 -> ChapterPosition.First
        totalChapters - 1 -> ChapterPosition.Last
        else -> ChapterPosition.Middle
      }
    return PlayerChapter(
      bookChapter.id,
      bookChapter.start,
      bookChapter.end,
      helper.formatChapterTime(bookChapter.start, true),
      helper.formatChapterTime(bookChapter.end, true),
      bookChapter.title,
      position,
    )
  }

  fun toPlaybackProgressPodcast(raw: RawPlaybackProgress): PlaybackProgress {
    val position = raw.position / 1000
    val duration = (raw.duration / 1000).toFloat()
    val buffered = (raw.bufferedPosition / 1000)
    val formattedPosition = helper.formatChapterTime(position.toDouble())
    val formattedDuration = helper.formatChapterTime(duration.toDouble())
    val formattedBuffered = if (duration > 0) buffered / duration else 0f
    val progress = if (duration > 0) position / duration else 0f
    return PlaybackProgress(
      position = formattedPosition,
      duration = formattedDuration,
      bufferedPosition = formattedBuffered,
      progress = progress,
    )
  }

  fun toPlaybackProgressBook(uiState: PlayerUiState, raw: RawPlaybackProgress): PlaybackProgress {
    val currentChapter = uiState.currentChapter
    val currentTrack = uiState.currentTrack
    val duration =
      if (currentChapter != null) {
        (currentChapter.endTimeSeconds - currentChapter.startTimeSeconds)
      } else {
        currentTrack.duration
      }

    val position =
      if (currentChapter != null) {
        if (uiState.playerTracks.size > 1) {
          (((raw.position / 1000) + currentTrack.startOffset) - currentChapter.startTimeSeconds)
            .toFloat()
        } else {
          ((raw.position / 1000) - currentChapter.startTimeSeconds).toFloat()
        }
      } else {
        ((raw.position / 1000) - currentTrack.startOffset).toFloat()
      }

    val buffered = (raw.bufferedPosition / 1000)

    val formattedPosition = helper.formatChapterTime(position.toDouble())
    val formattedDuration = helper.formatChapterTime(duration)
    val formattedBuffered = if (duration > 0) buffered / duration.toFloat() else 0f

    val progress = if (duration > 0) position / duration.toFloat() else 0f

    return PlaybackProgress(
      position = formattedPosition,
      duration = formattedDuration,
      bufferedPosition = formattedBuffered,
      progress = progress,
    )
  }
}
