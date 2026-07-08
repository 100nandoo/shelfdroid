package dev.halim.shelfdroid.core.data.screen.rssfeeds

import dev.halim.shelfdroid.core.data.GenericState

data class RssFeedsUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: RssFeedsApiState = RssFeedsApiState.Idle,
  val feeds: List<RssFeedUi> = emptyList(),
) {
  data class RssFeedUi(
    val id: String,
    val title: String,
    val slug: String,
    val entityType: String,
    val episodeCount: Int,
    val preventIndexing: Boolean,
    val updatedAtText: String,
    val publicFeedUrl: String,
    val coverUrl: String,
    val ownerName: String?,
    val ownerEmail: String?,
    val episodes: List<EpisodeUi>,
  )

  data class EpisodeUi(
    val id: String,
    val title: String,
    val publishedAtText: String?,
  )
}

sealed interface RssFeedsApiState {
  data object Idle : RssFeedsApiState

  data object Loading : RssFeedsApiState

  data object CloseSuccess : RssFeedsApiState

  data class CloseFailure(val message: String?) : RssFeedsApiState
}
