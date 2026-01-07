package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.data.GenericState

data class PodcastUiState(
  val state: GenericState = GenericState.Loading,
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val description: String = "",
  val canAddEpisode: Boolean = false,
  val episodes: List<Episode> = emptyList(),
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
)

data class Episode(
  val episodeId: String = "",
  val title: String = "",
  val publishedAt: String = "",
  val progress: Float = 0f,
  val isFinished: Boolean = false,
  val isPlaying: Boolean = false,
  val download: DownloadUiState = DownloadUiState(),
)
