package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.DisplayPrefs
import kotlinx.serialization.Serializable

data class HomeUiState(
  val homeState: HomeState = HomeState.Loading,
  val displayPrefs: DisplayPrefs = DisplayPrefs(),
  val currentPage: Int = 0,
  val librariesUiState: List<LibraryUiState> = emptyList(),
)

data class LibraryUiState(
  val id: String = "",
  val name: String = "",
  val isBook: Boolean = true,
  val books: List<BookUiState> = emptyList(),
  val podcasts: List<PodcastUiState> = emptyList(),
)

@Serializable
data class BookUiState(
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val isDownloaded: Boolean = false,
  val trackIndexes: List<Int> = emptyList(),
)

data class PodcastUiState(
  val id: String,
  val author: String,
  val title: String,
  val cover: String,
  val episodeCount: Int,
  val unfinishedCount: Int,
  val downloadedCount: Int,
  val unfinishedAndDownloadCount: Int,
)

sealed class HomeState {
  data object Loading : HomeState()

  data object Success : HomeState()

  data class Failure(val errorMessage: String?) : HomeState()
}
