package dev.halim.shelfdroid.download

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class ShelfDownloadService :
  DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.downloads,
    0,
  ) {

  companion object {
    private const val JOB_ID = 1
    const val FOREGROUND_NOTIFICATION_ID = 1
    const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
  }

  @Inject lateinit var downloadManager: Lazy<DownloadManager>

  @Inject lateinit var downloadNotificationHelper: Lazy<DownloadNotificationHelper>

  @Inject
  lateinit var bookBatchProgressNotificationBuilder: Lazy<BookBatchProgressNotificationBuilder>

  override fun getDownloadManager(): DownloadManager {
    return downloadManager.get()
  }

  override fun getScheduler(): Scheduler {
    return PlatformScheduler(this, JOB_ID)
  }

  override fun getForegroundNotification(
    downloads: List<Download>,
    notMetRequirements: Int,
  ): Notification {
    bookBatchProgressNotificationBuilder
      .get()
      .build(
        context = this,
        channelId = DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        iconResId = R.drawable.download,
        downloadManager = downloadManager.get(),
        downloads = downloads,
      )
      ?.let {
        return it
      }

    val message =
      downloads
        .groupBy { download ->
          DownloadNotificationPayload.fromDownload(download).groupKey(download.request.id)
        }
        .values
        .joinToString(separator = "\n") { groupedDownloads ->
          val payload = DownloadNotificationPayload.fromDownload(groupedDownloads.first())
          val downloadedTracks = groupedDownloads.count { it.state == Download.STATE_COMPLETED }
          val totalTracks = payload.trackCount

          if (payload.isBookBatch() && totalTracks != null && totalTracks > 1) {
            "${payload.displayTitle()} $downloadedTracks/$totalTracks"
          } else {
            payload.displayTitle()
          }
        }
    return downloadNotificationHelper
      .get()
      .buildProgressNotification(
        this,
        R.drawable.download,
        null,
        message,
        downloads,
        notMetRequirements,
      )
  }
}
