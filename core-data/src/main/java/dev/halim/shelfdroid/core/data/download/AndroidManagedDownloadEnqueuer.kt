package dev.halim.shelfdroid.core.data.download

import android.app.DownloadManager
import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidManagedDownloadEnqueuer
@Inject
constructor(@ApplicationContext private val context: Context) : ManagedDownloadEnqueuer {

  override fun enqueue(request: ManagedDownloadRequest): Long {
    val androidRequest =
      DownloadManager.Request(request.url.toUri())
        .setTitle(request.title)
        .setNotificationVisibility(request.notificationVisibility)
        .setDestinationInExternalPublicDir(request.destinationDirectory, request.filename)

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return manager.enqueue(androidRequest)
  }
}
