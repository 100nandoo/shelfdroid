package dev.halim.shelfdroid.core.data.screen.player

import android.util.Base64
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.PlayRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import kotlinx.serialization.json.Json

class PlayerRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val apiService: ApiService,
) {

  fun book(id: String): PlayerUiState {
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
      )
    } else PlayerUiState(state = PlayerState.Hidden(Error("Item not found")))
  }

  fun podcast(itemId: String, episodeId: String): PlayerUiState {
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

  suspend fun playBook(id: String): Boolean {
    val deviceInfo = DeviceInfo("ShelfDroid", generateNanoId())
    val request =
      PlayRequest(deviceInfo = deviceInfo, forceDirectPlay = true, forceTranscode = false)
    val result = apiService.playBook(id, request)
    return result.isSuccess
  }

  suspend fun playPodcast(itemId: String, episodeId: String): Boolean {
    val deviceInfo = DeviceInfo("ShelfDroid", generateNanoId())
    val request =
      PlayRequest(deviceInfo = deviceInfo, forceDirectPlay = true, forceTranscode = false)
    val result = apiService.playPodcast(itemId, episodeId, request)
    return result.isSuccess
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
)
