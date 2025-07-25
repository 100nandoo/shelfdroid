package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.PlayRequest
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.Device
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.database.BookmarkEntity
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerMapper
@Inject
constructor(
  private val helper: Helper,
  private val dataStoreManager: DataStoreManager,
  private val finder: PlayerFinder,
  private val device: Device,
) {

  suspend fun toPlayerTrack(audioTrack: AudioTrack): PlayerTrack {
    val url = helper.generateContentUrl(audioTrack.contentUrl)
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
    val position = raw.positionMs / 1000
    val duration = (raw.durationMs / 1000).toFloat()
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
    val duration = finder.bookDuration(uiState)

    val position = finder.bookChapterPosition(uiState, raw.positionMs)
    val buffered = (raw.bufferedPosition / 1000)

    val formattedPosition = helper.formatChapterTime(position)
    val formattedDuration = helper.formatChapterTime(duration)
    val formattedBuffered = if (duration > 0) buffered / duration.toFloat() else 0f

    val progress = if (duration > 0) (position / duration).toFloat() else 0f

    return PlaybackProgress(
      position = formattedPosition,
      duration = formattedDuration,
      bufferedPosition = formattedBuffered,
      progress = progress,
    )
  }

  fun toPlayerBookmark(entity: BookmarkEntity): PlayerBookmark {
    val readableTime = helper.formatChapterTime(entity.time.toDouble())
    return PlayerBookmark(entity.title, readableTime, entity.time)
  }

  suspend fun toPlayRequest(): PlayRequest {
    val deviceInfo =
      DeviceInfo(
        deviceId = getDeviceId(),
        manufacturer = device.manufacturer,
        model = device.model,
        osVersion = device.osVersion,
        sdkVersion = device.sdkVersion,
        clientName = device.clientName,
        clientVersion = device.clientVersion,
      )

    return PlayRequest(
      deviceInfo = deviceInfo,
      mediaPlayer = device.mediaPlayer,
      forceTranscode = true,
      forceDirectPlay = true,
    )
  }

  private suspend fun getDeviceId(): String =
    withContext(Dispatchers.IO) { dataStoreManager.getDeviceId() }
}
