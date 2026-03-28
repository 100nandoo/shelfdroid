package dev.halim.shelfdroid.core.data.screen.backups

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class BackupDownloader @Inject constructor(@ApplicationContext private val context: Context) {

  fun download(backup: BackupsUiState.BackupItem) {
    val request =
      DownloadManager.Request(backup.downloadUrl.toUri())
        .setTitle(backup.filename)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, backup.filename)

    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    dm.enqueue(request)
  }
}
