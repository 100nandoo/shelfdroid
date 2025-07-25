package dev.halim.shelfdroid.media.download

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
import dev.halim.shelfdroid.media.R
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
    return downloadNotificationHelper
      .get()
      .buildProgressNotification(
        this,
        R.drawable.download,
        null,
        null,
        downloads,
        notMetRequirements,
      )
  }
}
