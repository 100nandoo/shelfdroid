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
  val chapterTitle: String? = null,
  val showChapterNavigation: Boolean = false,
  val previousChapterEnabled: Boolean = false,
  val nextChapterEnabled: Boolean = false,
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
    chapterTitle = "The Adventure of the Speckled Band",
    showChapterNavigation = true,
    previousChapterEnabled = false,
    nextChapterEnabled = true,
    coverResource = R.drawable.sherlock_holmes_portrait_with_pipe,
  )

internal fun PlayerUiState.toPlayerWidgetData(): PlayerWidgetData? =
  if (id.isBlank()) {
    null
  } else {
    val chapterIndex = currentChapter?.let(playerChapters::indexOf) ?: -1
    val chapterIsAvailable = chapterIndex in playerChapters.indices
    val showChapterNavigation = playerChapters.size > 1 && chapterIsAvailable
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
      chapterTitle =
        currentChapter?.title?.takeIf { chapterIsAvailable && it.isNotBlank() },
      showChapterNavigation = showChapterNavigation,
      previousChapterEnabled = showChapterNavigation && chapterIndex > 0,
      nextChapterEnabled = showChapterNavigation && chapterIndex < playerChapters.lastIndex,
    )
  }

internal fun Flow<PlayerUiState>.playerWidgetData(): Flow<PlayerWidgetData?> = map {
  it.toPlayerWidgetData()
}
  .distinctUntilChanged()
