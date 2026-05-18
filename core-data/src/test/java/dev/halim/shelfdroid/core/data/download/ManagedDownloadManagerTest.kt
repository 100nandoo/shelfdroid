package dev.halim.shelfdroid.core.data.download

import android.app.DownloadManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedDownloadManagerTest {

  @Test
  fun enqueue_usesPublicDownloadsDirectoryAndCompletionNotification() {
    val enqueuer = FakeManagedDownloadEnqueuer()
    val manager = ManagedDownloadManager(enqueuer)
    val download =
      ManagedDownload(
        url = "https://example.com/backups/test.audiobookshelf",
        title = "test.audiobookshelf",
        filename = "test.audiobookshelf",
      )

    manager.enqueue(download)

    assertEquals(
      ManagedDownloadRequest(
        url = "https://example.com/backups/test.audiobookshelf",
        title = "test.audiobookshelf",
        filename = "test.audiobookshelf",
        destinationDirectory = ManagedDownloadManager.PUBLIC_DOWNLOADS_DIRECTORY,
        notificationVisibility = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
      ),
      enqueuer.request,
    )
  }
}

private class FakeManagedDownloadEnqueuer : ManagedDownloadEnqueuer {
  var request: ManagedDownloadRequest? = null

  override fun enqueue(request: ManagedDownloadRequest): Long {
    this.request = request
    return 1L
  }
}
