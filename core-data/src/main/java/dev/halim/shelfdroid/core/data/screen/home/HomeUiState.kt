package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.Prefs
import kotlinx.serialization.Serializable

data class HomeUiState(
  val homeState: HomeState = HomeState.Loading,
  val prefs: Prefs = Prefs(),
  val currentPage: Int = 0,
  val librariesUiState: List<LibraryUiState> = emptyList(),
)

data class LibraryUiState(
  val id: String = "",
  val name: String = "",
  val isBookLibrary: Boolean = true,
  val books: List<BookUiState> = emptyList(),
  val podcasts: List<PodcastUiState> = emptyList(),
)

@Serializable
data class BookUiState(
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val duration: Double = 0.0,
  val addedAt: Long = 0,
  val isDownloaded: Boolean = false,
  val trackIndexes: List<Int> = emptyList(),
  val progressLastUpdate: Long = 0,
)

data class PodcastUiState(
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val addedAt: Long = 0,
  val episodeCount: Int = 0,
  val unfinishedCount: Int = 0,
  val downloadedCount: Int = 0,
  val unfinishedAndDownloadCount: Int = 0,
)

sealed class HomeState {
  data object Loading : HomeState()

  data object Success : HomeState()

  data class Failure(val errorMessage: String?) : HomeState()
}
