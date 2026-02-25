package dev.halim.shelfdroid.core

data class DownloadUiState(
  val state: DownloadState = DownloadState.Unknown,
  val id: String = "",
  val url: String = "",
  val title: String = "",
)

data class MultipleTrackDownloadUiState(
  val state: DownloadState = DownloadState.Unknown,
  val items: List<DownloadUiState> = emptyList(),
)

sealed interface DownloadState {
  data object Downloading : DownloadState

  data object Completed : DownloadState

  data object Queued : DownloadState

  data object Failed : DownloadState

  data object Incomplete : DownloadState

  data object Unknown : DownloadState

  fun isDownloaded() = this is Completed
}
