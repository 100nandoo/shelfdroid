package dev.halim.shelfdroid.core.data.screen.player

import android.util.Base64
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.DeviceInfo
import dev.halim.core.network.request.PlayRequest
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject

class PlayerRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val apiService: ApiService,
) {

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

  fun item(id: String): PlayerUiState {
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

  suspend fun play(id: String) {
    val deviceInfo = DeviceInfo("ShelfDroid", generateNanoId())
    val request =
      PlayRequest(deviceInfo = deviceInfo, forceDirectPlay = true, forceTranscode = false)
    val result = apiService.play(id, request)
    result.onSuccess {}
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
