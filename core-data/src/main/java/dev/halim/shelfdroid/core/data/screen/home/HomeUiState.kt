package dev.halim.shelfdroid.core.data.screen.home

import kotlinx.serialization.Serializable

data class LibraryUiState(val id: String = "", val name: String = "")

data class HomeUiState(
  val homeState: HomeState = HomeState.Loading,
  val librariesUiState: List<LibraryUiState> = emptyList(),
  val libraryItemsUiState: Map<Int, List<ShelfdroidMediaItem>> = emptyMap(),
)

abstract class ShelfdroidMediaItem {
  abstract val id: String
  abstract val author: String
  abstract val title: String
  abstract val cover: String
  abstract val seekTime: Long
}

@Serializable
data class BookUiState(
  override val id: String = "",
  override val author: String = "",
  override val title: String = "",
  override val cover: String = "",
  override val seekTime: Long = 0L,
  val progress: Float = 0f,
) : ShelfdroidMediaItem()

data class PodcastUiState(
  override val id: String,
  override val author: String,
  override val title: String,
  override val cover: String,
  override val seekTime: Long = 0L,
  val episodeCount: Int,
) : ShelfdroidMediaItem()

sealed class HomeState {
  data object Loading : HomeState()

  data object Success : HomeState()

  data class Failure(val errorMessage: String?) : HomeState()
}
