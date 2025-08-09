package dev.halim.shelfdroid.core

data class DownloadUiState(
  val state: DownloadState = DownloadState.Unknown,
  val id: String = "",
  val url: String = "",
  val title: String = "",
)

sealed class DownloadState {
  data object Downloading : DownloadState()

  data object Completed : DownloadState()

  data object Queued : DownloadState()

  data object Failed : DownloadState()

  data object Unknown : DownloadState()

  fun isDownloaded() = this is Completed
}
