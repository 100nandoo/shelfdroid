package dev.halim.shelfdroid.core.data.screen.searchpodcast

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

data class SearchPodcastUi(
  val id: Int = 0,
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val genre: String = "",
  val episodeCount: Int = 0,
  val feedUrl: String = "",
  val pageUrl: String = "",
  val explicit: Boolean = false,
)
