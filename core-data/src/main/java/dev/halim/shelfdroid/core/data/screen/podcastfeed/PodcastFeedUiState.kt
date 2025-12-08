package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.shelfdroid.core.data.response.PodcastFolder
import dev.halim.shelfdroid.core.data.screen.searchpodcast.CreatePodcastResult

data class PodcastFeedUiState(
  val state: PodcastFeedState = PodcastFeedState.Loading,
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

sealed class PodcastFeedState {
  data object Loading : PodcastFeedState()

  data object ApiFeedSuccess : PodcastFeedState()

  data class ApiCreateSuccess(val result: CreatePodcastResult) : PodcastFeedState()

  data class Failure(val errorMessage: String?) : PodcastFeedState()
}
