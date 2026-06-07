package dev.halim.shelfdroid.download.storage.book

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import dev.halim.shelfdroid.download.notification.DownloadNotificationPayload
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
@Singleton
class BookDurableDownloadExporter
@Inject
constructor(
  private val cache: Cache,
  private val catalog: BookDurableDownloadCatalog,
) : DownloadManager.Listener {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val exportingIds = mutableSetOf<String>()

  override fun onDownloadChanged(
    downloadManager: DownloadManager,
    download: Download,
    finalException: Exception?,
  ) {
    if (download.state != Download.STATE_COMPLETED) return

    val payload = DownloadNotificationPayload.fromDownload(download)
    if (!payload.isBookTrack()) return

    val relativePath = payload.relativePath ?: return
    val filename = payload.filename ?: return

    synchronized(exportingIds) {
      if (!exportingIds.add(download.request.id)) return
    }

    scope.launch {
      try {
        exportFromCache(
          cacheKey = download.request.customCacheKey ?: download.request.id,
          relativePath = relativePath,
          filename = filename,
        )
        cache.removeResource(download.request.customCacheKey ?: download.request.id)
      } finally {
        synchronized(exportingIds) { exportingIds.remove(download.request.id) }
      }
    }
  }

  @Throws(IOException::class)
  private suspend fun exportFromCache(cacheKey: String, relativePath: String, filename: String) {
    val spans = cache.getCachedSpans(cacheKey).sortedBy { it.position }
    if (spans.isEmpty()) throw IOException("No cached spans found for $cacheKey")

    val expectedLength = ContentMetadata.getContentLength(cache.getContentMetadata(cacheKey))
    var nextPosition = 0L

    catalog.writeTrack(relativePath, filename) { output ->
      spans.forEach { span ->
        val file = span.file ?: throw IOException("Cache span file missing for $cacheKey")
        if (span.position != nextPosition) {
          throw IOException("Cache spans are incomplete for $cacheKey")
        }

        file.inputStream().use { input -> input.copyTo(output) }
        nextPosition += span.length
      }
    }

    if (expectedLength != C.LENGTH_UNSET.toLong() && nextPosition != expectedLength) {
      throw IOException("Expected $expectedLength bytes for $cacheKey but exported $nextPosition")
    }
  }
}
