package dev.halim.shelfdroid.core.data.download

import android.app.DownloadManager
import javax.inject.Inject

class ManagedDownloadManager @Inject constructor(private val enqueuer: ManagedDownloadEnqueuer) {

  companion object {
    const val PUBLIC_DOWNLOADS_DIRECTORY = "Download"
  }

  fun enqueue(download: ManagedDownload): Long = enqueuer.enqueue(toRequest(download))

  internal fun toRequest(download: ManagedDownload): ManagedDownloadRequest {
    return ManagedDownloadRequest(
      url = download.url,
      title = download.title,
      filename = download.filename,
      destinationDirectory = PUBLIC_DOWNLOADS_DIRECTORY,
      notificationVisibility = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
    )
  }
}

data class ManagedDownloadRequest(
  val url: String,
  val title: String,
  val filename: String,
  val destinationDirectory: String,
  val notificationVisibility: Int,
)
