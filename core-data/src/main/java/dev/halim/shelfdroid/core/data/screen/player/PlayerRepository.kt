package dev.halim.shelfdroid.core.data.screen.player

import android.util.Base64
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.PlayRequest
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PlayerRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val dataStoreManager: DataStoreManager,
  private val helper: Helper,
  private val apiService: ApiService,
) {

  suspend fun playBook(id: String): PlayerUiState {
    val playerUiState = book(id)
    if (playerUiState.state is PlayerState.Hidden) {
      return playerUiState
    }
    val result =
      runCatching {
          val deviceInfo = DeviceInfo("ShelfDroid", generateNanoId())
          val request =
            PlayRequest(deviceInfo = deviceInfo, forceDirectPlay = true, forceTranscode = false)

          val response = apiService.playBook(id, request)
          val playerTracks = response.audioTracks.map { toPlayerTrack(it) }

          val currentTrack = findCurrentPlayerTrack(playerTracks, response.currentTime)
          playerUiState.copy(
            playerTracks = playerTracks,
            currentTrack = currentTrack,
            currentTime = response.currentTime,
          )
        }
        .getOrNull()

    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Book")))
  }

  suspend fun playPodcast(itemId: String, episodeId: String): PlayerUiState {
    val playerUiState = podcast(itemId, episodeId)
    if (playerUiState.state is PlayerState.Hidden) {
      return playerUiState
    }
    val result =
      runCatching {
          val deviceInfo = DeviceInfo("ShelfDroid", generateNanoId())
          val request =
            PlayRequest(deviceInfo = deviceInfo, forceDirectPlay = true, forceTranscode = false)

          val response = apiService.playPodcast(itemId, episodeId, request)
          val playerTracks = response.audioTracks.map { toPlayerTrack(it) }

          val currentTrack = findCurrentPlayerTrack(playerTracks, response.currentTime)
          playerUiState.copy(playerTracks = playerTracks, currentTrack = currentTrack)
        }
        .getOrNull()
    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Podcast Episode")))
  }

  private fun book(id: String): PlayerUiState {
    val result = libraryItemRepo.byId(id)
    val progress = progressRepo.byLibraryItemId(id)
    return if (result != null) {
      val media = Json.decodeFromString<Book>(result.media)
      val chapters =
        media.chapters.mapIndexed { i, bookChapter ->
          toPlayerChapter(i, bookChapter, media.chapters.size)
        }
      val chapter = findCurrentPlayerChapter(chapters, progress?.currentTime ?: 0.0)
      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        author = result.author,
        title = chapter?.title ?: result.title,
        cover = result.cover,
        currentTime = progress?.currentTime ?: 0.0,
        playerChapters = chapters,
        currentChapter = chapter,
        playbackProgress =
          PlaybackProgress(
            duration = helper.formatChapterTime(chapter?.endTimeSeconds ?: 0.0, true)
          ),
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  private fun podcast(itemId: String, episodeId: String): PlayerUiState {
    val result = libraryItemRepo.byId(itemId)
    val progress = progressRepo.episodeById(episodeId)

    return if (result != null && result.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(result.media)

      val episode =
        media.episodes.find { it.id == episodeId }
          ?: return PlayerUiState(state = PlayerState.Hidden(Error("Failed to find episode")))
      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        episodeId = episode.id,
        author = result.author,
        title = episode.title,
        cover = result.cover,
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  private fun findCurrentPlayerTrack(
    playerTracks: List<PlayerTrack>,
    currentTime: Double,
  ): PlayerTrack {
    // Find the last track that starts at or before the current time
    return playerTracks.sortedBy { it.startOffset }.lastOrNull { it.startOffset <= currentTime }
      ?: playerTracks.first()
  }

  private fun findCurrentPlayerChapter(
    playerChapters: List<PlayerChapter>,
    currentTime: Double,
  ): PlayerChapter? {
    // Find the last chapter that starts at or before the current time
    if (playerChapters.isEmpty()) return null
    return playerChapters
      .sortedBy { it.startTimeSeconds }
      .lastOrNull { it.startTimeSeconds <= currentTime } ?: playerChapters.first()
  }

  fun changeChapter(uiState: PlayerUiState, target: Int): PlayerUiState {
    val chapters = uiState.playerChapters
    return if (target in chapters.indices) {
      val targetChapter = chapters[target]
      val targetTrack = findTrackFromChapter(uiState, targetChapter)
      val currentTime =
        if (uiState.playerTracks.size > 1) targetChapter.startTimeSeconds - targetTrack.startOffset
        else targetChapter.startTimeSeconds
      uiState.copy(
        title = targetChapter.title,
        currentChapter = targetChapter,
        currentTrack = targetTrack,
        currentTime = currentTime,
      )
    } else {
      uiState.copy(state = PlayerState.Hidden(Error("Failed to change chapter")))
    }
  }

  fun previousNextChapter(uiState: PlayerUiState, isPrevious: Boolean = true): PlayerUiState {
    val direction = if (isPrevious) -1 else +1
    val label = if (isPrevious) "previous" else "next"
    val targetChapter = uiState.playerChapters.indexOf(uiState.currentChapter) + direction
    return if (targetChapter in uiState.playerChapters.indices) {
      changeChapter(uiState, targetChapter)
    } else {
      uiState.copy(state = PlayerState.Hidden(Error("No $label chapter available")))
    }
  }

  private fun findTrackFromChapter(uiState: PlayerUiState, chapter: PlayerChapter): PlayerTrack {
    val tracks = uiState.playerTracks
    return if (tracks.size == 1) {
      return tracks.first()
    } else {
      val track = tracks.lastOrNull { it.startOffset <= chapter.startTimeSeconds } ?: tracks.first()
      track
    }
  }

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.token.first() }

  private suspend fun toPlayerTrack(audioTrack: AudioTrack): PlayerTrack {
    val url = helper.generateContentUrl(getToken(), audioTrack.contentUrl)
    return PlayerTrack(url, audioTrack.duration, audioTrack.startOffset)
  }

  private fun toPlayerChapter(
    index: Int,
    bookChapter: BookChapter,
    totalChapters: Int,
  ): PlayerChapter {
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

  fun toPlaybackProgress(raw: RawPlaybackProgress, uiState: PlayerUiState): PlaybackProgress {
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

    val progress = if (duration > 0) position / duration.toFloat() else 0f
    val bufferProgress = if (duration > 0) buffered / duration.toFloat() else 0f

    val formattedPosition = helper.formatChapterTime(position.toDouble())
    val formattedDuration = helper.formatChapterTime(duration)
    return PlaybackProgress(
      position = formattedPosition,
      duration = formattedDuration,
      bufferedPosition = bufferProgress,
      progress = progress,
    )
  }

  private fun generateNanoId(): String {
    val uuid = UUID.randomUUID()
    val byteBuffer = ByteBuffer.wrap(ByteArray(16))
    byteBuffer.putLong(uuid.mostSignificantBits)
    byteBuffer.putLong(uuid.leastSignificantBits)
    return Base64.encodeToString(
      byteBuffer.array(),
      Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
    )
  }
}

sealed class PlayerState {
  class Hidden(error: Error? = null) : PlayerState()

  data object TempHidden : PlayerState()

  data object Big : PlayerState()

  data object Small : PlayerState()
}

data class PlayerUiState(
  val state: PlayerState = PlayerState.Hidden(),
  val id: String = "",
  val episodeId: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val progress: Float = 0f,
  val currentTime: Double = 0.0,
  val playerTracks: List<PlayerTrack> = emptyList(),
  val currentTrack: PlayerTrack = PlayerTrack(),
  val playerChapters: List<PlayerChapter> = emptyList(),
  val currentChapter: PlayerChapter? = PlayerChapter(),
  val playbackProgress: PlaybackProgress = PlaybackProgress(),
)

data class PlayerTrack(
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
)

enum class ChapterPosition {
  First,
  Last,
  Middle,
}

data class RawPlaybackProgress(
  val position: Long = 0,
  val duration: Long = 0,
  val bufferedPosition: Long = 0,
)

data class PlaybackProgress(
  val position: String = "",
  val duration: String = "",
  val bufferedPosition: Float = 0f,
  val progress: Float = 0f,
)
