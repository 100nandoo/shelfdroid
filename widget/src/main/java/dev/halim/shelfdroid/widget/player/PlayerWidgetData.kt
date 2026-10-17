package dev.halim.shelfdroid.widget.player

import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.widget.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class PlayerWidgetData(
  val id: String,
  val episodeId: String,
  val title: String,
  val author: String,
  val cover: String,
  val playPauseEnabled: Boolean,
  val showPlayIcon: Boolean,
  val seekBackEnabled: Boolean,
  val seekForwardEnabled: Boolean,
  val coverResource: Int? = null,
)

internal val playerWidgetPreviewData =
  PlayerWidgetData(
    id = "preview",
    episodeId = "preview",
    title = "The Adventures of Sherlock Holmes",
    author = "Arthur Conan Doyle",
    cover = "",
    playPauseEnabled = true,
    showPlayIcon = true,
    seekBackEnabled = true,
    seekForwardEnabled = true,
    coverResource = R.drawable.sherlock_holmes_portrait_with_pipe,
  )

internal fun PlayerUiState.toPlayerWidgetData(): PlayerWidgetData? =
  if (id.isBlank()) {
    null
  } else {
    PlayerWidgetData(
      id = id,
      episodeId = episodeId,
      title = title,
      author = author,
      cover = cover,
      playPauseEnabled = playPause.enabled,
      showPlayIcon = playPause.showPlayIcon,
      seekBackEnabled = seekControls.seekBackEnabled,
      seekForwardEnabled = seekControls.seekForwardEnabled,
    )
  }

internal fun Flow<PlayerUiState>.playerWidgetData(): Flow<PlayerWidgetData?> = map {
  it.toPlayerWidgetData()
}
  .distinctUntilChanged()
