package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import kotlinx.serialization.Serializable

data class SearchPodcastUiState(
  val state: GenericState = GenericState.Idle,
  val result: List<SearchPodcastUi> = emptyList(),
)

@Serializable
data class SearchPodcastUi(
  val id: String = "",
  val itunesId: Int = 0,
  val itunesArtistId: Int? = null,
  val libraryId: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val genre: String = "",
  val episodeCount: Int = 0,
  val feedUrl: String = "",
  val pageUrl: String = "",
  val releaseDate: String = "",
  val explicit: Boolean = false,
  val isAdded: Boolean = false,
  val payload: PodcastFeedNavPayload = PodcastFeedNavPayload(),
)
