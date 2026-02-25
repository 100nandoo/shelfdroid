package dev.halim.shelfdroid.core.ui.event

sealed interface CommonDownloadEvent {
  data object Download : CommonDownloadEvent

  data object DeleteDownload : CommonDownloadEvent
}
