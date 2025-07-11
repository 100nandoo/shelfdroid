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
  val id: String = "",
  val title: String = "",
  val publishedAt: String = "",
  val progress: Float = 0f,
  val isFinished: Boolean = false,
)
