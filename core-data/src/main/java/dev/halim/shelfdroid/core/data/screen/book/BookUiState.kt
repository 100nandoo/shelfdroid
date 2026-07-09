package dev.halim.shelfdroid.core.data.screen.book

import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.MultipleTrackDownloadUiState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedUiState

data class BookUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: BookApiState = BookApiState.Idle,
  val author: String = "",
  val narrator: String = "",
  val title: String = "",
  val subtitle: String = "",
  val duration: String = "",
  val remaining: String = "",
  val cover: String = "",
  val description: String = "",
  val publishYear: String = "",
  val publisher: String = "",
  val genres: String = "",
  val language: String = "",
  val progress: Int = 0,
  val isEbook: Boolean = false,
  val isSingleTrack: Boolean = false,
  val canEdit: Boolean = false,
  val generatedRssFeed: GeneratedRssFeedUiState = GeneratedRssFeedUiState(),
  val download: DownloadUiState = DownloadUiState(),
  val downloads: MultipleTrackDownloadUiState = MultipleTrackDownloadUiState(),
)

sealed interface BookApiState {
  data object Idle : BookApiState

  data object OpenRssFeedLoading : BookApiState

  data object OpenRssFeedSuccess : BookApiState

  data class OpenRssFeedFailure(val message: String) : BookApiState

  data object CloseRssFeedLoading : BookApiState

  data object CloseRssFeedSuccess : BookApiState

  data class CloseRssFeedFailure(val message: String) : BookApiState
}
