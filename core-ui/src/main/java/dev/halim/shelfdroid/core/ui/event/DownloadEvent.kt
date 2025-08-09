package dev.halim.shelfdroid.core.ui.event

sealed class CommonDownloadEvent {
  data class Download(val downloadId: String, val url: String, val message: String) :
    CommonDownloadEvent()

  data class DeleteDownload(val downloadId: String) : CommonDownloadEvent()
}
