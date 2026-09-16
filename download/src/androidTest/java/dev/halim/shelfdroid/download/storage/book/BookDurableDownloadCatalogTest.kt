package dev.halim.shelfdroid.download.storage.book

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.download.storage.ReadableStoragePolicy
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDurableDownloadCatalogTest {
  @Test
  fun refresh_whenBookFileAppearsOutsideCatalog_rediscoversTrack() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val storagePolicy = ReadableStoragePolicy()
    val catalog = BookDurableDownloadCatalog(context, storagePolicy, Dispatchers.IO)
    val suffix = UUID.randomUUID().toString()
    val title = "Rediscovery $suffix"
    val author = "ShelfDroid"
    val filename = "$suffix.mp3"
    val relativePath = storagePolicy.bookRelativePath(title, author)

    assertTrue(catalog.trackUris(title, author, listOf(filename)).isEmpty())

    val uri =
      checkNotNull(
        context.contentResolver.insert(
          MediaStore.Downloads.EXTERNAL_CONTENT_URI,
          ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 0)
          },
        )
      )

    try {
      assertTrue(catalog.trackUris(title, author, listOf(filename)).isEmpty())

      catalog.refresh()

      assertTrue(catalog.trackUris(title, author, listOf(filename))[filename] != null)
    } finally {
      context.contentResolver.delete(uri, null, null)
    }
  }
}
