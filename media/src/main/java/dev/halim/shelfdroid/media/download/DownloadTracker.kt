package dev.halim.shelfdroid.media.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@SuppressLint("UnsafeOptInUsageError")
class DownloadTracker
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val downloadManager: DownloadManager,
) {
  fun download(url: String) {
    DownloadService.sendAddDownload(
      context,
      ShelfDownloadService::class.java,
      toDownloadRequest(url),
      false,
    )
  }

  private fun toDownloadRequest(url: String): DownloadRequest {
    return DownloadRequest.Builder(url, url.toUri()).build()
  }

  fun isDownloaded(url: String): Boolean {
    val download = downloadManager.downloadIndex.getDownload(url)
    return download?.state == Download.STATE_COMPLETED || download?.percentDownloaded == 100f
  }
}
