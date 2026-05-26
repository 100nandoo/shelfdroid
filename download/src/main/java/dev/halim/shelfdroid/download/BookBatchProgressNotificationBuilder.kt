package dev.halim.shelfdroid.download

import android.app.Notification
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class BookBatchProgressNotificationBuilder @Inject constructor(private val helper: Helper) {

  @OptIn(UnstableApi::class)
  fun build(
    context: Context,
    channelId: String,
    iconResId: Int,
    downloadManager: DownloadManager,
    downloads: List<Download>,
  ): Notification? {
    val groupedDownloads = downloads.groupBy { download ->
      DownloadNotificationPayload.fromDownload(download).groupKey(download.request.id)
    }

    if (groupedDownloads.size != 1) return null

    val activeBatchDownloads = groupedDownloads.values.first()
    val payload = DownloadNotificationPayload.fromDownload(activeBatchDownloads.first())
    val totalTracks = payload.trackCount

    if (!payload.isBookBatch() || totalTracks == null || totalTracks <= 1) return null

    val batchId = payload.batchId ?: return null
    val batchDownloads = buildList {
      downloadManager.downloadIndex.getDownloads().use { cursor ->
        while (cursor.moveToNext()) {
          val candidate = cursor.download
          val candidatePayload = DownloadNotificationPayload.fromDownload(candidate)
          if (candidatePayload.groupKey(candidate.request.id) == batchId) {
            add(candidate)
          }
        }
      }
    }

    if (batchDownloads.isEmpty()) return null

    val completedTracks = batchDownloads.count { it.state == Download.STATE_COMPLETED }
    val activeTracks = batchDownloads.count { it.state == Download.STATE_DOWNLOADING }
    val title = context.getString(R.string.download_book_batch_title, payload.displayTitle())
    val inProgressText =
      context.resources.getQuantityString(
        R.plurals.download_book_batch_parallel_downloads,
        activeTracks,
        activeTracks,
      )
    val contentText =
      context.getString(
        R.string.download_book_batch_status,
        completedTracks,
        totalTracks,
        inProgressText,
      )
    val progressPercent = (completedTracks * 100) / totalTracks
    val pendingIntent =
      helper.createOpenDetailIntent(
        payload.openDetailTargetId(batchDownloads.first().request.id),
        context,
      )

    return NotificationCompat.Builder(context, channelId)
      .setSmallIcon(iconResId)
      .setContentTitle(title)
      .setContentText(contentText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setShowWhen(false)
      .setProgress(100, progressPercent, false)
      .build()
  }
}
