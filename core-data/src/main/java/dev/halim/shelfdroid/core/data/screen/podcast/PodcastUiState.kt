package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.shelfdroid.core.data.GenericState

data class PodcastUiState(
  val state: GenericState = GenericState.Loading,
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val description: String = "",
  val episodes: List<Episode> = emptyList(),
)

data class Episode(
  val episodeId: String = "",
  val downloadId: String = "",
  val title: String = "",
  val publishedAt: String = "",
  val progress: Float = 0f,
  val isFinished: Boolean = false,
  val url: String = "",
  val downloadState: DownloadState = DownloadState.Unknown,
)

enum class DownloadState {
  Downloading,
  Completed,
  Queued,
  Failed,
  Unknown,
}
