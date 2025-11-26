package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.PodcastFolder

data class PodcastFeedUiState(
  val state: GenericState = GenericState.Loading,
  val title: String = "",
  val author: String = "",
  val feedUrl: String = "",
  val genres: List<String> = emptyList(),
  val type: String = "",
  val language: String = "",
  val explicit: Boolean = false,
  val description: String = "",
  val folders: List<PodcastFolder> = emptyList(),
  val selectedFolder: PodcastFolder = PodcastFolder("", ""),
  val path: String = "",
  val autoDownload: Boolean = false,
)
