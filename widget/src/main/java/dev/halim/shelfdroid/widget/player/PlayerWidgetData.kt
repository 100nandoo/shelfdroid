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
    val isEpisode = episodeId.isNotBlank()
    val chapters = if (isEpisode) episodeChapters else playerChapters
    val chapter = if (isEpisode) currentEpisodeChapter else currentChapter
    val chapterIndex = chapter?.let(chapters::indexOf) ?: -1
    val chapterIsAvailable = chapterIndex in chapters.indices
    val showChapterNavigation = chapters.size > 1 && chapterIsAvailable
    PlayerWidgetData(
      id = id,
      episodeId = episodeId,
      title = if (isEpisode) title else libraryItemTitle.ifBlank { title },
      author = if (isEpisode) libraryItemTitle.ifBlank { author } else author,
      cover = cover,
      playPauseEnabled = playPause.enabled,
      showPlayIcon = playPause.showPlayIcon,
      seekBackEnabled = seekControls.seekBackEnabled,
      seekForwardEnabled = seekControls.seekForwardEnabled,
      chapterTitle = chapter?.title?.takeIf { chapterIsAvailable && it.isNotBlank() },
      showChapterNavigation = showChapterNavigation,
      previousChapterEnabled = showChapterNavigation && chapterIndex > 0,
      nextChapterEnabled = showChapterNavigation && chapterIndex < chapters.lastIndex,
    )
  }

internal fun Flow<PlayerUiState>.playerWidgetData(): Flow<PlayerWidgetData?> = map {
  it.toPlayerWidgetData()
}
  .distinctUntilChanged()
