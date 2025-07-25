package dev.halim.shelfdroid.media.download

import android.content.Context
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.media.R
import dev.halim.shelfdroid.media.download.ShelfDownloadService.Companion.FOREGROUND_NOTIFICATION_ID
import javax.inject.Inject

@UnstableApi
class TerminalStateNotificationHelper
@Inject
constructor(
  @ApplicationContext context: Context,
  private val notificationHelper: DownloadNotificationHelper,
) : DownloadManager.Listener {

  private val appContext: Context = context.applicationContext
  private var nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1

  override fun onDownloadChanged(
    downloadManager: DownloadManager,
    download: Download,
    finalException: Exception?,
  ) {
    val notification =
      when (download.state) {
        Download.STATE_COMPLETED ->
          notificationHelper.buildDownloadCompletedNotification(
            appContext,
            R.drawable.download_done,
            null,
            Util.fromUtf8Bytes(download.request.data),
          )

        Download.STATE_FAILED ->
          notificationHelper.buildDownloadFailedNotification(
            appContext,
            R.drawable.download_failed,
            null,
            Util.fromUtf8Bytes(download.request.data),
          )

        else -> return
      }
    NotificationUtil.setNotification(appContext, nextNotificationId++, notification)
  }
}
