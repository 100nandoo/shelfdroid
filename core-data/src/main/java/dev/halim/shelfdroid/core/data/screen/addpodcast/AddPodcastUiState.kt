package dev.halim.shelfdroid.core.data.screen.addpodcast

import dev.halim.shelfdroid.core.data.response.PodcastFolder
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult

data class AddPodcastUiState(
  val state: AddPodcastState = AddPodcastState.Loading,
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

sealed class AddPodcastState {
  data object Loading : AddPodcastState()

  data object ApiFeedSuccess : AddPodcastState()

  data class ApiCreateSuccess(val result: CreatePodcastNavResult) : AddPodcastState()

  data class Failure(val errorMessage: String?) : AddPodcastState()
}
