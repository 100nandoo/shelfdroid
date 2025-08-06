package dev.halim.shelfdroid.core.data.screen.episode

import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.data.GenericState

data class EpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val title: String = "",
  val podcast: String = "",
  val publishedAt: String = "",
  val cover: String = "",
  val description: String = "",
  val progress: String = "",
  val download: DownloadUiState = DownloadUiState(),
)
