package dev.halim.shelfdroid.media.library

import android.app.Application
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.media3.datasource.DataSourceBitmapLoader
import coil3.ImageLoader
import coil3.disk.DiskCache
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = ArtworkTestApplication::class)
class MediaArtworkProviderTest {
  private val app = RuntimeEnvironment.getApplication() as ArtworkTestApplication
  private val repository = mock(LibraryItemRepository::class.java)
  private val cache =
    DiskCache.Builder().directory(app.cacheDir.resolve("test-covers").toOkioPath()).build()

  init {
    app.dependencies =
      object : MediaArtworkProvider.Dependencies, GeneratedComponent {
        override fun authState() = error("In-process requests do not need auth state")

        override fun libraryItems() = repository

        override fun imageLoader() = ImageLoader.Builder(app).diskCache(cache).build()

        override fun httpClient() =
          OkHttpClient.Builder().addInterceptor { throw IOException("Offline") }.build()
      }
  }

  @Test
  fun contentUriReadsTheCatalogItemsCachedCover() {
    val coverUrl = "https://example.invalid/api/items/book/cover?v=1"
    val coverBytes = byteArrayOf(1, 2, 3)
    val editor = requireNotNull(cache.openEditor(coverUrl))
    cache.fileSystem.write(editor.data) { write(coverBytes) }
    editor.commit()
    `when`(repository.byId("book")).thenReturn(book(coverUrl))
    val uri = MediaArtworkProvider.uriFor(app, "book", coverUrl)
    val descriptor = requireNotNull(app.contentResolver.openFileDescriptor(uri, "r"))
    ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
      assertArrayEquals(coverBytes, it.readBytes())
    }
  }

  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun uncachedOfflineCoverReturnsBundledArtwork() {
    val coverUrl = "https://example.invalid/api/items/book/cover?v=2"
    `when`(repository.byId("book")).thenReturn(book(coverUrl))
    val uri = MediaArtworkProvider.uriFor(app, "book", coverUrl)
    val bitmap =
      DataSourceBitmapLoader.Builder(app).build().loadBitmap(uri).get(5, TimeUnit.SECONDS)
    assertEquals(128, bitmap.width)
    assertEquals(128, bitmap.height)
  }

  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun publishedCoverUriReturnsBundledArtworkAfterCatalogCoverChanges() {
    val oldCover = book("https://example.invalid/api/items/book/cover?v=1")
    `when`(repository.byId("book")).thenReturn(oldCover)
    val publishedUri = MediaArtworkProvider.uriFor(app, oldCover.id, oldCover.cover)

    val updatedCover = oldCover.copy(cover = "https://example.invalid/api/items/book/cover?v=2")
    `when`(repository.byId("book")).thenReturn(updatedCover)

    val bitmap =
      DataSourceBitmapLoader.Builder(app).build().loadBitmap(publishedUri).get(5, TimeUnit.SECONDS)
    assertEquals(128, bitmap.width)
    assertEquals(128, bitmap.height)
  }

  private fun book(coverUrl: String) =
    LibraryItemEntity(
      id = "book",
      libraryId = "lib",
      author = "",
      authorFirstLast = "",
      authorLastFirst = "",
      title = "",
      description = "",
      cover = coverUrl,
      updatedAt = 1,
      rssFeed = null,
      isBook = 1,
      inoId = "",
      duration = 0.0,
      addedAt = 0,
    )

  @Test
  fun deletedAndMalformedUrisFailThroughTheContentResolver() {
    val uri = Uri.parse("content://${app.packageName}.car-artwork/deleted")
    assertThrows(FileNotFoundException::class.java) {
      app.contentResolver.openFileDescriptor(uri, "r")
    }
    assertThrows(FileNotFoundException::class.java) {
      app.contentResolver.openFileDescriptor(uri.buildUpon().appendPath("extra").build(), "r")
    }
    val cover = book("https://example.invalid/api/items/book/cover?v=1")
    `when`(repository.byId(cover.id)).thenReturn(cover)
    val malformedVersion =
      MediaArtworkProvider.uriFor(app, cover.id)
        .buildUpon()
        .appendQueryParameter("v", "!!!")
        .build()
    assertThrows(FileNotFoundException::class.java) {
      app.contentResolver.openFileDescriptor(malformedVersion, "r")
    }
  }
}

class ArtworkTestApplication :
  Application(), GeneratedComponentManager<MediaArtworkProvider.Dependencies> {
  lateinit var dependencies: MediaArtworkProvider.Dependencies

  override fun generatedComponent() = dependencies
}
