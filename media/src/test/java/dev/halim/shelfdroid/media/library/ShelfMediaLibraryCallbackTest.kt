package dev.halim.shelfdroid.media.library

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import dagger.Lazy
import dev.halim.core.network.ApiService
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.data.auth.AuthStateRepository
import dev.halim.shelfdroid.core.data.di.DatabaseModule
import dev.halim.shelfdroid.core.data.library.BookLocalDataSource
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.data.library.PodcastEpisodeRepository
import dev.halim.shelfdroid.core.data.library.PodcastLocalDataSource
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.media.playback.PlayerStore
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShelfMediaLibraryCallbackTest {
  @Test
  fun rootAndLimitedTabsAreAvailableWithoutReadingTheCatalog() {
    val context = RuntimeEnvironment.getApplication()
    val catalog = mock(CarCatalogRepository::class.java)
    val callback =
      ShelfMediaLibraryCallback(
        context,
        Lazy<PlayerStore> { error("Root does not need the player store") },
        Lazy { catalog },
        CarMediaItemMapper(context),
      )
    callback.attach(CoroutineScope(Dispatchers.Unconfined))
    val session = mock(MediaLibrarySession::class.java)
    val browser = mock(MediaSession.ControllerInfo::class.java)
    val params =
      LibraryParams.Builder()
        .setExtras(
          Bundle().apply {
            putInt("androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_LIMIT", 2)
          }
        )
        .build()

    assertEquals(
      "car:root",
      callback.onGetLibraryRoot(session, browser, params).get().value?.mediaId,
    )
    val tabs =
      callback.onGetChildren(session, browser, "car:root", 0, Int.MAX_VALUE, null).get().value
    assertEquals(listOf("car:books", "car:podcasts"), tabs?.map { it.mediaId })
    val page = callback.onGetChildren(session, browser, "car:root", 1, 1, null).get().value
    assertEquals(listOf("car:podcasts"), page?.map { it.mediaId })
    verifyNoInteractions(catalog)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun onlyAnEmptyTabWaitsAndReturnsAfterThreeSeconds() = runTest {
    val context = RuntimeEnvironment.getApplication()
    val repository = mock(CarCatalogRepository::class.java)
    val mapper = CarMediaItemMapper(context)
    val row = mapper.folder(CarMediaId.Podcasts)
    val refresh = CompletableDeferred<Unit>()
    `when`(repository.listChildren(CarMediaId.Books)).thenReturn(emptyList())
    `when`(repository.listChildren(CarMediaId.Podcasts)).thenReturn(listOf(row))
    `when`(repository.startRefreshIfStale()).thenReturn(refresh)
    val callback =
      ShelfMediaLibraryCallback(
          context,
          Lazy<PlayerStore> { error("Browse does not need the player store") },
          Lazy { repository },
          mapper,
        )
        .also { it.attach(backgroundScope) }
    val session = mock(MediaLibrarySession::class.java)
    val browser = mock(MediaSession.ControllerInfo::class.java)
    val cold = callback.onGetChildren(session, browser, "car:books", 0, Int.MAX_VALUE, null)
    val warm = callback.onGetChildren(session, browser, "car:podcasts", 0, Int.MAX_VALUE, null)
    runCurrent()
    assertEquals(listOf(row), warm.get().value)
    advanceTimeBy(2_999)
    assertFalse(cold.isDone)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(emptyList<MediaItem>(), cold.get().value)
    refresh.complete(Unit)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun newSessionTabWaitsForItsOwnRefresh() = runTest {
    val context = RuntimeEnvironment.getApplication()
    val repository = mock(CarCatalogRepository::class.java)
    val mapper = CarMediaItemMapper(context)
    val previousRefresh = CompletableDeferred<Unit>()
    val newRefresh = CompletableDeferred<Unit>()
    val newRow = mapper.folder(CarMediaId.DownloadedBooks)
    `when`(repository.listChildren(CarMediaId.Books))
      .thenReturn(emptyList(), emptyList(), listOf(newRow))
    `when`(repository.startRefreshIfStale()).thenReturn(previousRefresh, newRefresh)
    val callback =
      ShelfMediaLibraryCallback(
          context,
          Lazy<PlayerStore> { error("Browse does not need the player store") },
          Lazy { repository },
          mapper,
        )
        .also { it.attach(backgroundScope) }
    val session = mock(MediaLibrarySession::class.java)
    val browser = mock(MediaSession.ControllerInfo::class.java)
    val previous = callback.onGetChildren(session, browser, "car:books", 0, Int.MAX_VALUE, null)
    runCurrent()
    assertFalse(previous.isDone)

    val current = callback.onGetChildren(session, browser, "car:books", 0, Int.MAX_VALUE, null)
    runCurrent()
    assertFalse(current.isDone)
    newRefresh.complete(Unit)
    runCurrent()
    assertTrue(current.isDone)
    assertEquals(listOf(newRow), current.get().value)
    assertFalse(previous.isDone)
    previousRefresh.cancel()
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun unsubscribeDuringInitialLoadPreventsObservation() = runTest {
    val context = RuntimeEnvironment.getApplication()
    val repository = mock(CarCatalogRepository::class.java)
    val initial = CompletableDeferred<List<MediaItem>>()
    `when`(repository.listChildren(CarMediaId.Books)).thenAnswer { invocation ->
      @Suppress("UNCHECKED_CAST")
      val continuation =
        invocation.rawArguments.last() as kotlin.coroutines.Continuation<List<MediaItem>>
      val load: suspend () -> List<MediaItem> = { initial.await() }
      load.startCoroutineUninterceptedOrReturn(continuation)
    }
    val callback =
      ShelfMediaLibraryCallback(
          context,
          Lazy<PlayerStore> { error("Browse does not need the player store") },
          Lazy { repository },
          CarMediaItemMapper(context),
        )
        .also { it.attach(backgroundScope) }
    val session = mock(MediaLibrarySession::class.java)
    val browser = mock(MediaSession.ControllerInfo::class.java)
    val subscribe = callback.onSubscribe(session, browser, "car:books", null)
    runCurrent()
    assertFalse(subscribe.isDone)

    callback.onUnsubscribe(session, browser, "car:books").get()
    initial.complete(emptyList())
    runCurrent()
    assertEquals(LibraryResult.RESULT_SUCCESS, subscribe.get().resultCode)
    verify(repository, never()).observeChanges(CarMediaId.Books)
    verifyNoInteractions(session)
  }

  @Test
  fun largeLibraryAndPodcastListsKeepEveryEntryReachable() = withCatalog {
    db.transaction {
      repeat(52) { index ->
        val id = "library-$index"
        library(id, isBook = true, order = index.toLong())
        item(id, "book-$index", "Book $index", isBook = true)
      }
      library("podcasts", isBook = false)
      repeat(52) { index ->
        item("podcasts", "podcast-$index", "Podcast $index", isBook = false)
      }
    }

    assertEquals(
      (0..51).map { "car:book:book-$it" },
      playableDescendants("car:books"),
    )
    val podcastIds =
      children("car:podcasts").flatMap { group ->
        assertEquals(group, getItem(group.mediaId).value)
        children(group.mediaId).also { assertTrue(it.size <= 50) }.map { it.mediaId }
      }
    assertEquals((0..51).map { "car:podcast:podcast-$it" }.toSet(), podcastIds.toSet())
  }

  @Test
  fun fiftyLongEpisodeRowsFitTheLegacyBrowserParcel() = withCatalog {
    val longText = "Long metadata for a car browser ".repeat(40)
    library("podcasts", isBook = false)
    item("podcasts", "podcast", longText, isBook = false, author = longText, cover = "cover")
    episodes.replace(
      "podcast",
      (1..50).map {
        PodcastEpisode(libraryItemId = "podcast", id = "episode-$it", title = longText)
      },
    )

    val rows = children("car:podcast:podcast")
    assertEquals(50, rows.size)
    // Media3's legacy bridge uses this package-private conversion before applying its byte limit.
    val convert =
      Class.forName("androidx.media3.session.LegacyConversions")
        .getDeclaredMethod(
          "convertToBrowserItem",
          MediaItem::class.java,
          Bitmap::class.java,
        )
        .apply { isAccessible = true }
    val parcel = Parcel.obtain()
    try {
      rows.forEach { parcel.writeParcelable(convert.invoke(null, it, null) as Parcelable, 0) }
      assertTrue("Legacy payload was ${parcel.dataSize()} bytes", parcel.dataSize() < 256 * 1024)
    } finally {
      parcel.recycle()
    }
  }

  @Test
  fun tabsResolveZeroOneAndManyLibrariesIndependently() = withCatalog {
    assertEquals(emptyList<String>(), children("car:books").map { it.mediaId })
    assertEquals(emptyList<String>(), children("car:podcasts").map { it.mediaId })

    library("books", isBook = true)
    library("podcasts", isBook = false)
    item("books", "book", "Book", isBook = true)
    item("podcasts", "podcast", "Podcast", isBook = false)
    assertEquals(listOf("car:book:book"), children("car:books").map { it.mediaId })
    assertEquals(listOf("car:podcast:podcast"), children("car:podcasts").map { it.mediaId })

    library("second-books", isBook = true, order = 1)
    assertEquals(
      listOf("car:library:books", "car:library:second-books"),
      children("car:books").map { it.mediaId },
    )
    assertEquals(listOf("car:book:book"), children("car:library:books").map { it.mediaId })
    assertEquals(listOf("car:podcast:podcast"), children("car:podcasts").map { it.mediaId })
  }

  @Test
  fun largeBooksAndEpisodeListsKeepEveryEntryReachable() = withCatalog {
    library("books", isBook = true)
    library("podcasts", isBook = false)
    item("podcasts", "podcast", "Podcast", isBook = false)
    db.transaction {
      repeat(2_501) { item("books", "book-$it", "Same title", isBook = true) }
    }
    val expectedBookIds = (0..2_500).map { "car:book:book-$it" }.toSet()
    assertEquals(expectedBookIds, playableDescendants("car:books").toSet())

    val date = Instant.parse("2024-06-01T12:00:00Z").toEpochMilli()
    episodes.replace(
      "podcast",
      (0..75).map {
        PodcastEpisode(
          libraryItemId = "podcast",
          id = "episode-$it",
          title = "Same episode title",
          publishedAt = date.takeIf { _ -> it < 75 },
        )
      },
    )
    assertEquals(
      listOf("2024", "Unknown date"),
      children("car:podcast:podcast").map {
        it.mediaMetadata.title?.toString()
      },
    )
    assertEquals(
      (0..75).map { "car:episode:podcast:episode-$it" }.toSet(),
      playableDescendants("car:podcast:podcast").toSet(),
    )
    assertEquals(
      LibraryResult.RESULT_ERROR_BAD_VALUE,
      getItem("car:episode:other:episode-0").resultCode,
    )
    assertEquals(
      "car:episode:podcast:episode-0",
      getItem("car:episode:podcast:episode-0").value?.mediaId,
    )
    assertEquals("podcast|episode-0", getItem("podcast|episode-0").value?.mediaId)
  }

  @Test
  fun downloadsAlwaysHasTwoGroupsAndShowsOnlyMatchingAudio() = withCatalog {
    assertEquals(
      listOf("car:downloaded-books", "car:downloaded-episodes"),
      children("car:downloads").map { it.mediaId },
    )
    assertEquals(emptyList<String>(), children("car:downloaded-books").map { it.mediaId })
    assertEquals(emptyList<String>(), children("car:downloaded-episodes").map { it.mediaId })

    library("podcasts", isBook = false)
    item("podcasts", "podcast", "Podcast", isBook = false)
    val cached =
      listOf(
        PodcastEpisode(libraryItemId = "podcast", id = "saved", title = "Saved"),
        PodcastEpisode(libraryItemId = "podcast", id = "other", title = "Other"),
      )
    episodes.replace("podcast", cached)
    runBlocking {
      `when`(downloads.podcastDownloadedEpisodeIds("Podcast", cached)).thenReturn(setOf("saved"))
    }
    assertEquals(
      listOf("car:episode:podcast:saved"),
      children("car:downloaded-episodes").map { it.mediaId },
    )
  }

  @Test
  fun downloadsUpdatesForCompleteBooksAndRemovedEpisodes() = withCatalog {
    library("books", isBook = true)
    item("books", "full", "Full", isBook = true)
    item("books", "partial", "Partial", isBook = true)
    val tracks =
      listOf("one.mp3", "two.mp3").mapIndexed { index, name ->
        AudioTrack(index = index, metadata = FileMetadata(filename = name))
      }
    `when`(books.byId("full")).thenReturn(Book(audioTracks = tracks))
    `when`(books.byId("partial")).thenReturn(Book(audioTracks = tracks))
    runBlocking {
      `when`(downloads.isBookDownloaded("Full", "", tracks)).thenReturn(true)
      `when`(downloads.isBookDownloaded("Partial", "", tracks)).thenReturn(false)
    }
    assertEquals(listOf("car:book:full"), children("car:downloaded-books").map { it.mediaId })

    library("podcasts", isBook = false)
    item("podcasts", "podcast", "Podcast", isBook = false)
    val saved = PodcastEpisode(libraryItemId = "podcast", id = "saved", title = "Saved")
    episodes.replace("podcast", listOf(saved))
    runBlocking {
      `when`(downloads.podcastDownloadedEpisodeIds("Podcast", listOf(saved)))
        .thenReturn(setOf("saved"))
    }
    assertEquals(
      LibraryResult.RESULT_SUCCESS,
      callback
        .onSubscribe(session, browser, "car:downloaded-episodes", null)
        .get(5, TimeUnit.SECONDS)
        .resultCode,
    )
    assertEquals(
      listOf("car:episode:podcast:saved"),
      children("car:downloaded-episodes").map { it.mediaId },
    )

    runBlocking {
      `when`(downloads.podcastDownloadedEpisodeIds("Podcast", listOf(saved))).thenReturn(emptySet())
    }
    downloadChanges.value++
    verify(session, timeout(5_000))
      .notifyChildrenChanged(browser, "car:downloaded-episodes", 0, null)
    assertTrue(children("car:downloaded-episodes").isEmpty())
  }

  @Test
  fun losingAccessClearsAnOpenListAndWithholdsDirectLookups() = withCatalog {
    library("books", isBook = true)
    item("books", "book", "Book", isBook = true)
    `when`(browser.controllerVersion).thenReturn(1)
    assertEquals(
      LibraryResult.RESULT_SUCCESS,
      callback.onSubscribe(session, browser, "car:books", null).get(5, TimeUnit.SECONDS).resultCode,
    )
    assertEquals(listOf("car:book:book"), children("car:books").map { it.mediaId })

    token.value = ""
    verify(session, timeout(5_000)).notifyChildrenChanged(browser, "car:books", 0, null)
    assertEquals(emptyList<String>(), children("car:books").map { it.mediaId })
    assertEquals(emptyList<String>(), children("car:downloads").map { it.mediaId })
    assertEquals(LibraryResult.RESULT_ERROR_BAD_VALUE, getItem("car:book:book").resultCode)
    assertEquals("car:books", getItem("car:books").value?.mediaId)
  }

  private fun withCatalog(test: Fixture.() -> Unit) {
    Fixture().use(test)
  }

  private class Fixture : AutoCloseable {
    val context = RuntimeEnvironment.getApplication()
    private val driver = DatabaseModule.provideSqlDriver(context)
    val db = DatabaseModule.provideSqlDelightAppDatabase(driver, DatabaseModule.provideMapAdapter())
    val episodes = PodcastEpisodeRepository(db, Json)
    val downloads = mock(DownloadRepo::class.java)
    val downloadChanges = MutableStateFlow(0)
    val books = mock(BookLocalDataSource::class.java)
    val token = MutableStateFlow("test-user")
    val session = mock(MediaLibrarySession::class.java)
    val browser =
      mock(MediaSession.ControllerInfo::class.java).also {
        `when`(it.packageName).thenReturn(context.packageName)
      }
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val mapper = CarMediaItemMapper(context)
    private val api = mock(ApiService::class.java)
    private val helper = mock(Helper::class.java)
    private val progress = ProgressRepository(db, helper)
    private val repository =
      CarCatalogRepository(
        context,
        LibraryRepository(api, db, Json),
        mock(AuthStateRepository::class.java).also { `when`(it.token).thenReturn(token) },
        mock(LibraryDataRepository::class.java),
        LibraryItemRepository(
          api,
          db,
          helper,
          Json,
          downloads,
          progress,
          books,
          mock(PodcastLocalDataSource::class.java),
          episodes,
        ),
        episodes,
        progress,
        downloads.also { `when`(it.durableDownloads).thenReturn(downloadChanges) },
        mapper,
      )
    val callback =
      ShelfMediaLibraryCallback(
          context,
          Lazy<PlayerStore> { error("Browse does not need the player store") },
          Lazy { repository },
          mapper,
        )
        .also { it.attach(scope) }

    fun children(id: String): List<MediaItem> =
      requireNotNull(
        callback
          .onGetChildren(session, browser, id, 0, Int.MAX_VALUE, null)
          .get(5, TimeUnit.SECONDS)
          .value
      )

    fun getItem(id: String): LibraryResult<MediaItem> =
      callback.onGetItem(session, browser, id).get(5, TimeUnit.SECONDS)

    fun playableDescendants(id: String): List<String> {
      val rows = children(id)
      assertTrue(rows.size <= 50)
      return rows.flatMap { row ->
        if (row.mediaMetadata.isBrowsable == true) {
          assertEquals(row, getItem(row.mediaId).value)
          playableDescendants(row.mediaId)
        } else {
          listOf(row.mediaId)
        }
      }
    }

    fun library(id: String, isBook: Boolean, order: Long = 0) {
      db.libraryEntityQueries.insert(LibraryEntity(id, id, "[]", if (isBook) 1 else 0, order, ""))
    }

    fun item(
      libraryId: String,
      id: String,
      title: String,
      isBook: Boolean,
      author: String = "",
      cover: String = "",
    ) {
      db.libraryItemEntityQueries.insert(
        LibraryItemEntity(
          id,
          libraryId,
          author,
          "",
          "",
          title,
          "",
          cover,
          0,
          null,
          if (isBook) 1 else 0,
          "",
          0.0,
          0,
        )
      )
    }

    override fun close() {
      scope.cancel()
      driver.close()
    }
  }
}
