package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.shelfdroid.core.data.GenericState

data class AddEpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val episodes: List<Episode> = emptyList(),
)

data class Episode(
  val title: String,
  val description: String,
  val pubDate: String,
  val publishedAt: Long,
  val url: String,
  val state: AddEpisodeDownloadState,
)

enum class AddEpisodeDownloadState {
  Downloaded,
  ToBeDownloaded,
  NotDownloaded,
}
