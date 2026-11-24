package dev.halim.shelfdroid.media.library

import android.app.Application
import android.net.Uri
import android.os.ParcelFileDescriptor
import coil3.ImageLoader
import coil3.disk.DiskCache
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import java.io.FileNotFoundException
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = ArtworkTestApplication::class)
class CarArtworkProviderTest {
  private val app = RuntimeEnvironment.getApplication() as ArtworkTestApplication
  private val repository = mock(LibraryItemRepository::class.java)
  private val cache =
    DiskCache.Builder().directory(app.cacheDir.resolve("test-covers").toOkioPath()).build()

  init {
    app.dependencies =
      object : CarArtworkProvider.Dependencies, GeneratedComponent {
        override fun authState() = error("In-process requests do not need auth state")

        override fun libraryItems() = repository

        override fun imageLoader() = ImageLoader.Builder(app).diskCache(cache).build()
      }
  }

  @Test
  fun contentUriReadsTheCatalogItemsCachedCover() {
    val coverUrl = "https://example.invalid/api/items/book/cover?v=1"
    val coverBytes = byteArrayOf(1, 2, 3)
    val editor = requireNotNull(cache.openEditor(coverUrl))
    cache.fileSystem.write(editor.data) { write(coverBytes) }
    editor.commit()
    `when`(repository.byId("book"))
      .thenReturn(
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
      )
    val uri = Uri.parse("content://${app.packageName}.car-artwork/book")
    val descriptor = requireNotNull(app.contentResolver.openFileDescriptor(uri, "r"))
    ParcelFileDescriptor.AutoCloseInputStream(descriptor).use {
      assertArrayEquals(coverBytes, it.readBytes())
    }
  }

  @Test
  fun staleAndMalformedUrisFailThroughTheContentResolver() {
    val uri = Uri.parse("content://${app.packageName}.car-artwork/deleted")
    assertThrows(FileNotFoundException::class.java) {
      app.contentResolver.openFileDescriptor(uri, "r")
    }
    assertThrows(FileNotFoundException::class.java) {
      app.contentResolver.openFileDescriptor(uri.buildUpon().appendPath("extra").build(), "r")
    }
  }
}

class ArtworkTestApplication :
  Application(), GeneratedComponentManager<CarArtworkProvider.Dependencies> {
  lateinit var dependencies: CarArtworkProvider.Dependencies

  override fun generatedComponent() = dependencies
}
