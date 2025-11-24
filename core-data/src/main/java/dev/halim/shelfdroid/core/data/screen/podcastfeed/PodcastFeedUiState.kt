package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.shelfdroid.core.data.GenericState

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
  val folder: String = "",
  val path: String = "",
)
