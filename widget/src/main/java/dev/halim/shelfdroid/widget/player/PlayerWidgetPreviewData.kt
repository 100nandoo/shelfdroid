package dev.halim.shelfdroid.widget.player

import dev.halim.shelfdroid.widget.R

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
    isPreview = true,
  )
