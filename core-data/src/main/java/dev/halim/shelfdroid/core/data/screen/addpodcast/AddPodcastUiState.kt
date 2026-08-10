package dev.halim.shelfdroid.core.data.screen.addpodcast

import dev.halim.shelfdroid.core.data.catalog.LibraryFolder
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
  val folders: List<LibraryFolder> = emptyList(),
  val selectedFolder: LibraryFolder = LibraryFolder("", ""),
  val path: String = "",
  val autoDownload: Boolean = false,
)

sealed interface AddPodcastState {
  data object Loading : AddPodcastState

  data object ApiSourceFeedSuccess : AddPodcastState

  data class ApiCreateSuccess(val result: CreatePodcastNavResult) : AddPodcastState

  data class Failure(val errorMessage: String?) : AddPodcastState
}
