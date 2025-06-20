package dev.halim.shelfdroid.core.data.screen.player

import android.util.Base64
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.PlayRequest
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

          val currentTrack = findCurrentPlayerTrack(playerTracks, playerUiState.currentTime)
          playerUiState.copy(playerTracks = playerTracks, currentTrack = currentTrack)
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

          val currentTrack = findCurrentPlayerTrack(playerTracks, playerUiState.currentTime)
          playerUiState.copy(playerTracks = playerTracks, currentTrack = currentTrack)
        }
        .getOrNull()
    return result ?: PlayerUiState(state = PlayerState.Hidden(Error("Can't Play Podcast Episode")))
  }

  private fun book(id: String): PlayerUiState {
    val result = libraryItemRepo.byId(id)
    val progress = progressRepo.byLibraryItemId(id)
    return if (result != null) {
      PlayerUiState(
        state = PlayerState.Small,
        id = result.id,
        author = result.author,
        title = result.title,
        cover = result.cover,
        progress = progress?.progress?.toFloat() ?: 0f,
        currentTime = progress?.currentTime ?: 0.0,
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
        author = result.author,
        title = episode.title,
        cover = result.cover,
        progress = progress?.progress?.toFloat() ?: 0f,
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  fun findCurrentPlayerTrack(playerTracks: List<PlayerTrack>, currentTime: Double): PlayerTrack {
    // Find the last track that starts at or before the current time
    return playerTracks.sortedBy { it.startOffset }.lastOrNull { it.startOffset <= currentTime }
      ?: playerTracks.first()
  }

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.token.first() }

  private suspend fun toPlayerTrack(audioTrack: AudioTrack): PlayerTrack {
    val url = helper.generateContentUrl(getToken(), audioTrack.contentUrl)
    return PlayerTrack(url, audioTrack.startOffset)
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
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val progress: Float = 0f,
  val currentTime: Double = 0.0,
  val currentTrack: PlayerTrack = PlayerTrack(),
  val playerTracks: List<PlayerTrack> = emptyList(),
)

data class PlayerTrack(val url: String = "", val startOffset: Double = 0.0)
