package dev.halim.shelfdroid.media.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@SuppressLint("UnsafeOptInUsageError")
class DownloadTracker @Inject constructor(@ApplicationContext private val context: Context) {
  fun download(id: String, url: String) {
    DownloadService.sendAddDownload(
      context,
      ShelfDownloadService::class.java,
      toDownloadRequest(id, url),
      false,
    )
  }

  fun delete(id: String) {
    DownloadService.sendRemoveDownload(context, ShelfDownloadService::class.java, id, false)
  }

  private fun toDownloadRequest(id: String, url: String): DownloadRequest {
    return DownloadRequest.Builder(id, url.toUri()).build()
  }
}
