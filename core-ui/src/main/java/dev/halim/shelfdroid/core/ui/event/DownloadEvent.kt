package dev.halim.shelfdroid.core.ui.event

sealed class CommonDownloadEvent {
  data object Download : CommonDownloadEvent()

  data object DeleteDownload : CommonDownloadEvent()
}
