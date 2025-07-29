package dev.halim.shelfdroid.core.data.screen.home

import kotlinx.serialization.Serializable

data class LibraryUiState(
  val id: String = "",
  val name: String = "",
  val isBook: Boolean = true,
  val books: List<BookUiState> = emptyList(),
  val podcasts: List<PodcastUiState> = emptyList(),
)

data class HomeUiState(
  val homeState: HomeState = HomeState.Loading,
  val librariesUiState: List<LibraryUiState> = emptyList(),
)

@Serializable
data class BookUiState(
  val id: String = "",
  val author: String = "",
  val title: String = "",
  val cover: String = "",
)

data class PodcastUiState(
  val id: String,
  val author: String,
  val title: String,
  val cover: String,
  val unfinishedEpisodeCount: Int,
)

sealed class HomeState {
  data object Loading : HomeState()

  data object Success : HomeState()

  data class Failure(val errorMessage: String?) : HomeState()
}
