package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.shelfdroid.core.data.GenericState

data class AddEpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val downloadEpisodeState: GenericState = GenericState.Idle,
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val episodes: List<AddEpisode> = emptyList(),
  val filterState: AddEpisodeFilterState = AddEpisodeFilterState(),
)

data class AddEpisode(
  val episodeId: String,
  val title: String,
  val description: String,
  val publishedDate: String,
  val publishedAt: Long,
  val url: String,
  val state: AddEpisodeDownloadState,
)

data class AddEpisodeFilterState(
  val titleQuery: String = "",
  val publishedStartDateMillis: Long? = null,
  val publishedEndDateMillis: Long? = null,
  val hideDownloaded: Boolean = true,
)

enum class AddEpisodeDownloadState {
  Downloaded,
  ToBeDownloaded,
  NotDownloaded,
}
