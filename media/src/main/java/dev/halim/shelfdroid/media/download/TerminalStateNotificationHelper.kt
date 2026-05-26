package dev.halim.shelfdroid.media.download

import android.content.Context
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.halim.shelfdroid.download.DownloadNotificationPayload
import dev.halim.shelfdroid.download.ShelfDownloadService.Companion.FOREGROUND_NOTIFICATION_ID
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.media.R
import javax.inject.Inject

@UnstableApi
class TerminalStateNotificationHelper
@Inject
constructor(
  @ApplicationContext context: Context,
  private val notificationHelper: DownloadNotificationHelper,
  private val helper: Helper,
) : DownloadManager.Listener {

  private val appContext: Context = context.applicationContext
  private var nextNotificationId = FOREGROUND_NOTIFICATION_ID + 1
  private val sentBatchTerminalStates = mutableMapOf<String, Int>()

  override fun onDownloadChanged(
    downloadManager: DownloadManager,
    download: Download,
    finalException: Exception?,
  ) {
    val payload = DownloadNotificationPayload.fromDownload(download)

    if (payload.isBookBatch()) {
      showBatchTerminalNotification(downloadManager = downloadManager, payload = payload)
      return
    }

    val pendingIntent =
      helper.createOpenDetailIntent(payload.openDetailTargetId(download.request.id), appContext)
    val notification =
      when (download.state) {
        Download.STATE_COMPLETED ->
          notificationHelper.buildDownloadCompletedNotification(
            appContext,
            R.drawable.download_done,
            pendingIntent,
            payload.terminalDisplayText(),
          )

        Download.STATE_FAILED ->
          notificationHelper.buildDownloadFailedNotification(
            appContext,
            R.drawable.download_failed,
            pendingIntent,
            payload.terminalDisplayText(),
          )

        else -> return
      }
    NotificationUtil.setNotification(appContext, nextNotificationId++, notification)
  }

  private fun showBatchTerminalNotification(
    downloadManager: DownloadManager,
    payload: DownloadNotificationPayload,
  ) {
    val batchId = payload.batchId ?: return
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

    if (batchDownloads.isEmpty()) {
      sentBatchTerminalStates.remove(batchId)
      return
    }

    if (
      batchDownloads.any {
        it.state == Download.STATE_QUEUED || it.state == Download.STATE_DOWNLOADING
      }
    ) {
      sentBatchTerminalStates.remove(batchId)
      return
    }

    val terminalState =
      when {
        batchDownloads.all { it.state == Download.STATE_COMPLETED } -> Download.STATE_COMPLETED
        batchDownloads.all { it.state == Download.STATE_FAILED } -> Download.STATE_FAILED
        batchDownloads.any { it.state == Download.STATE_FAILED } -> Download.STATE_FAILED
        else -> return
      }

    if (sentBatchTerminalStates[batchId] == terminalState) return

    val completedCount = batchDownloads.count { it.state == Download.STATE_COMPLETED }
    val failedCount = batchDownloads.count { it.state == Download.STATE_FAILED }
    val trackCount = payload.trackCount ?: batchDownloads.size
    val message =
      when {
        terminalState == Download.STATE_COMPLETED -> payload.terminalDisplayText()
        terminalState == Download.STATE_FAILED && failedCount > 0 && completedCount > 0 ->
          "${payload.displayTitle()} ($completedCount completed, $failedCount failed)"
        terminalState == Download.STATE_FAILED && trackCount > 1 ->
          "${payload.displayTitle()} ($failedCount/$trackCount tracks failed)"
        else -> payload.displayTitle()
      }

    val pendingIntent = helper.createOpenDetailIntent(batchId, appContext)
    val notification =
      when (terminalState) {
        Download.STATE_COMPLETED ->
          notificationHelper.buildDownloadCompletedNotification(
            appContext,
            R.drawable.download_done,
            pendingIntent,
            message,
          )
        Download.STATE_FAILED ->
          notificationHelper.buildDownloadFailedNotification(
            appContext,
            R.drawable.download_failed,
            pendingIntent,
            message,
          )
        else -> return
      }

    NotificationUtil.setNotification(appContext, nextNotificationId++, notification)
    sentBatchTerminalStates[batchId] = terminalState
  }
}
