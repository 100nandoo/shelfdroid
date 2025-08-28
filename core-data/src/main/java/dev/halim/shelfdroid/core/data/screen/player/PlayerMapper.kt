package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.PlayRequest
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.Device
import dev.halim.shelfdroid.core.PlaybackProgress
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerTrack
import dev.halim.shelfdroid.core.RawPlaybackProgress
import dev.halim.shelfdroid.core.database.BookmarkEntity
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerMapper
@Inject
constructor(
  private val helper: Helper,
  private val dataStoreManager: DataStoreManager,
  private val device: Device,
  private val state: PlayerInternalStateHolder,
) {

  suspend fun toPlayerTrack(audioTrack: AudioTrack): PlayerTrack {
    val url = helper.generateContentUrl(audioTrack.contentUrl)
    return PlayerTrack(audioTrack.index, url, audioTrack.duration, audioTrack.startOffset)
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

  fun toPlaybackProgress(raw: RawPlaybackProgress): PlaybackProgress {
    val position = raw.positionMs / 1000
    val duration = state.duration().toLong()
    val buffered = (raw.bufferedPosition / 1000)
    val formattedBuffered = if (duration > 0) buffered / duration.toFloat() else 0f
    val progress = if (duration > 0) position / duration.toFloat() else 0f
    return PlaybackProgress(
      position = position,
      duration = duration,
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
