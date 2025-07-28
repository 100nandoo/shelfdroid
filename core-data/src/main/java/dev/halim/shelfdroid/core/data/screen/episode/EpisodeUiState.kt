package dev.halim.shelfdroid.core.data.screen.episode

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.podcast.DownloadState

data class EpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val title: String = "",
  val podcast: String = "",
  val publishedAt: String = "",
  val cover: String = "",
  val description: String = "",
  val progress: String = "",
  val downloadState: DownloadState = DownloadState.Unknown,
  val downloadId: String = "",
  val url: String = "",
)
