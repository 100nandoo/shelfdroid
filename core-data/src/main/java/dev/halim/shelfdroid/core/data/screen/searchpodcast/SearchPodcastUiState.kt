package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import kotlinx.serialization.Serializable

data class SearchPodcastUiState(
  val state: SearchState = SearchState.Blank,
  val result: List<SearchPodcastUi> = emptyList(),
)

sealed class SearchState {
  data object Blank : SearchState()

  data object Loading : SearchState()

  data object Success : SearchState()

  data class Failure(val errorMessage: String?) : SearchState()
}

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
