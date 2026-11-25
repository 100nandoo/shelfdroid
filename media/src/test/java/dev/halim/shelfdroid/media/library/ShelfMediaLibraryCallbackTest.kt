package dev.halim.shelfdroid.media.library

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.legacy.MediaBrowserCompat
import dagger.Lazy
import dev.halim.core.network.ApiService
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.PlayPauseControlStateHolder
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
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
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.download.DownloadMapper
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.download.storage.ReadableStoragePolicy
import dev.halim.shelfdroid.download.storage.book.BookDurableDownloadCatalog
import dev.halim.shelfdroid.download.storage.podcast.PodcastDurableDownloadCatalog
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.media.playback.PlayerStore
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

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

    val root = callback.onGetLibraryRoot(session, browser, params).get()
    assertEquals("car:root", root.value?.mediaId)
    val rootExtras =
      Class.forName("androidx.media3.session.LegacyConversions")
        .getDeclaredMethod("convertToRootHints", LibraryParams::class.java)
        .apply { isAccessible = true }
        .invoke(null, root.params) as Bundle
    assertEquals(
      MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
      rootExtras.getInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE),
    )
    assertEquals(
      MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
      rootExtras.getInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE),
    )
    assertEquals(
      2,
      rootExtras.getInt("androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_LIMIT"),
    )
    assertFalse(params.extras.containsKey(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE))
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
    val fixture = BrowseCallbackFixture(backgroundScope)
    val row = fixture.mapper.folder(CarMediaId.Podcasts)
    val refresh = CompletableDeferred<Unit>()
    `when`(fixture.repository.listChildren(CarMediaId.Books)).thenReturn(emptyList())
    `when`(fixture.repository.listChildren(CarMediaId.Podcasts)).thenReturn(listOf(row))
    `when`(fixture.repository.startRefreshIfStale()).thenReturn(refresh)

    val cold = fixture.children("car:books")
    val warm = fixture.children("car:podcasts")
    runCurrent()
    assertEquals(listOf(row), warm.get().value)
    val repeat = fixture.children("car:books")
    runCurrent()
    assertTrue(repeat.isDone)
    assertEquals(emptyList<MediaItem>(), repeat.get().value)

    advanceTimeBy(2_999)
    assertFalse(cold.isDone)
    advanceTimeBy(1)
    runCurrent()
    assertEquals(emptyList<MediaItem>(), cold.get().value)
    refresh.complete(Unit)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun coldTabReloadsRowsWhenAnotherRefreshHasJustCompleted() = runTest {
    val fixture = BrowseCallbackFixture(backgroundScope)
    val row = fixture.mapper.folder(CarMediaId.Podcasts)
    `when`(fixture.repository.listChildren(CarMediaId.Podcasts))
      .thenReturn(emptyList(), listOf(row))
    `when`(fixture.repository.startRefreshIfStale()).thenReturn(null)

    val result = fixture.children("car:podcasts")
    runCurrent()

    assertTrue(result.isDone)
    assertEquals(listOf(row), result.get().value)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun coldTabDeadlineDoesNotCancelRefreshScheduling() = runTest {
    val fixture = BrowseCallbackFixture(backgroundScope)
    val row = fixture.mapper.folder(CarMediaId.Podcasts)
    val refreshStarted = CompletableDeferred<Unit>()
    `when`(fixture.repository.listChildren(CarMediaId.Podcasts)).thenReturn(emptyList())
    `when`(fixture.repository.startRefreshIfStale()).thenAnswer { invocation ->
      @Suppress("UNCHECKED_CAST")
      val continuation = invocation.rawArguments.last() as Continuation<Job>
      val startRefresh: suspend () -> Job = {
        delay(3_001)
        `when`(fixture.repository.listChildren(CarMediaId.Podcasts)).thenReturn(listOf(row))
        refreshStarted.complete(Unit)
        refreshStarted
      }
      startRefresh.startCoroutineUninterceptedOrReturn(continuation)
    }

    val result = fixture.children("car:podcasts")
    runCurrent()
    advanceTimeBy(3_000)
    runCurrent()
    assertTrue(result.isDone)
    assertEquals(emptyList<MediaItem>(), result.get().value)
    assertFalse(refreshStarted.isCompleted)

    advanceTimeBy(1)
    runCurrent()
    assertTrue(refreshStarted.isCompleted)
    val next = fixture.children("car:podcasts")
    runCurrent()
    assertEquals(listOf(row), next.get().value)
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun newSessionTabWaitsForItsOwnRefresh() = runTest {
    val fixture = BrowseCallbackFixture(backgroundScope)
    val previousRefresh = CompletableDeferred<Unit>()
    val newRefresh = CompletableDeferred<Unit>()
    val newRow = fixture.mapper.folder(CarMediaId.DownloadedBooks)
    `when`(fixture.repository.listChildren(CarMediaId.Books))
      .thenReturn(emptyList(), emptyList(), listOf(newRow))
    `when`(fixture.repository.startRefreshIfStale()).thenReturn(previousRefresh, newRefresh)
    val previous = fixture.children("car:books")
    runCurrent()
    assertFalse(previous.isDone)

    val current = fixture.children("car:books")
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
    val fixture = BrowseCallbackFixture(backgroundScope)
    val initial = CompletableDeferred<List<MediaItem>>()
    `when`(fixture.repository.listChildren(CarMediaId.Books)).thenAnswer { invocation ->
      @Suppress("UNCHECKED_CAST")
      val continuation = invocation.rawArguments.last() as Continuation<List<MediaItem>>
      val load: suspend () -> List<MediaItem> = { initial.await() }
      load.startCoroutineUninterceptedOrReturn(continuation)
    }
    val subscribe =
      fixture.callback.onSubscribe(fixture.session, fixture.browser, "car:books", null)
    runCurrent()
    assertFalse(subscribe.isDone)

    fixture.callback.onUnsubscribe(fixture.session, fixture.browser, "car:books").get()
    initial.complete(emptyList())
    runCurrent()
    assertEquals(LibraryResult.RESULT_SUCCESS, subscribe.get().resultCode)
    verify(fixture.repository, never()).observeChanges(CarMediaId.Books)
    verifyNoInteractions(fixture.session)
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
  @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
  fun fiftyLongEpisodeRowsFitTheLegacyBrowserParcel() = withCatalog {
    val longText = "Long metadata for a car browser ".repeat(40)
    library("podcasts", isBook = false)
    item("podcasts", "podcast", longText, isBook = false, author = longText, cover = "cover")
    val cached =
      (1..50).map {
        PodcastEpisode(
          libraryItemId = "podcast",
          id = "episode-$it",
          title = longText,
          audioTrack = AudioTrack(metadata = FileMetadata(filename = "episode-$it.mp3")),
        )
      }
    episodes.replace("podcast", cached)
    runBlocking {
      `when`(downloads.isPodcastEpisodeDownloaded(longText, "episode-1.mp3")).thenReturn(true)
    }
    db.progressEntityQueries.insert(
      ProgressEntity("progress", "podcast", "episode-1", "", .42, 100.0, 42.0, 0, 0)
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
      val legacyRows = rows.map { convert.invoke(null, it, null) as MediaBrowserCompat.MediaItem }
      val legacyExtras =
        requireNotNull(
          legacyRows.single { it.mediaId == "car:episode:podcast:episode-1" }.description.extras
        )
      assertEquals(
        MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED,
        legacyExtras.getInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS),
      )
      assertEquals(
        .42,
        legacyExtras.getDouble(MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE),
        0.0,
      )
      assertEquals(
        MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED,
        legacyExtras.getLong(MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS),
      )
      legacyRows.forEach { parcel.writeParcelable(it, 0) }
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

    library("second-podcasts", isBook = false, order = 1)
    assertEquals(
      listOf("car:library:podcasts", "car:library:second-podcasts"),
      children("car:podcasts").map { it.mediaId },
    )
    assertEquals(listOf("car:podcast:podcast"), children("car:library:podcasts").map { it.mediaId })
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
    val bookStatuses = children("car:books").associate { it.mediaId to it.downloadStatus() }
    assertEquals(MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED, bookStatuses["car:book:full"])
    assertEquals(
      MediaConstants.EXTRAS_VALUE_STATUS_NOT_DOWNLOADED,
      bookStatuses["car:book:partial"],
    )
    assertEquals(
      MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED,
      children("car:downloaded-books").single().downloadStatus(),
    )

    library("podcasts", isBook = false)
    item("podcasts", "podcast", "Podcast", isBook = false)
    val saved =
      PodcastEpisode(
        libraryItemId = "podcast",
        id = "saved",
        title = "Saved",
        audioTrack = AudioTrack(metadata = FileMetadata(filename = "saved.mp3")),
      )
    episodes.replace("podcast", listOf(saved))
    runBlocking {
      `when`(downloads.podcastDownloadedEpisodeIds("Podcast", listOf(saved)))
        .thenReturn(setOf("saved"))
      `when`(downloads.isPodcastEpisodeDownloaded("Podcast", "saved.mp3")).thenReturn(true)
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
    assertEquals(
      MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED,
      getItem("car:episode:podcast:saved").value?.downloadStatus(),
    )
    assertEquals(
      MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED,
      children("car:downloaded-episodes").single().downloadStatus(),
    )
    `when`(browser.controllerVersion)
      .thenReturn(MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION)
    assertEquals(
      LibraryResult.RESULT_SUCCESS,
      callback
        .onSubscribe(session, browser, "car:podcast:podcast", null)
        .get(5, TimeUnit.SECONDS)
        .resultCode,
    )

    runBlocking {
      `when`(downloads.podcastDownloadedEpisodeIds("Podcast", listOf(saved))).thenReturn(emptySet())
      `when`(downloads.isPodcastEpisodeDownloaded("Podcast", "saved.mp3")).thenReturn(false)
    }
    downloadChanges.value++
    verify(session, timeout(5_000))
      .notifyChildrenChanged(browser, "car:downloaded-episodes", 0, null)
    verify(session, timeout(5_000)).notifyChildrenChanged(browser, "car:podcast:podcast", 1, null)
    assertTrue(children("car:downloaded-episodes").isEmpty())
    assertEquals(
      MediaConstants.EXTRAS_VALUE_STATUS_NOT_DOWNLOADED,
      children("car:podcast:podcast").single().downloadStatus(),
    )
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

  @Test
  fun browseRowsAndDirectLookupSurviveDownloadStatusFailures() = withCatalog {
    library("books", isBook = true)
    item("books", "book", "Book", isBook = true)
    `when`(books.byId("book")).thenThrow(IllegalStateException("Unable to read audio details"))
    val notDownloaded = MediaConstants.EXTRAS_VALUE_STATUS_NOT_DOWNLOADED
    assertEquals(notDownloaded, children("car:books").single().downloadStatus())
    assertEquals(notDownloaded, getItem("car:book:book").value?.downloadStatus())

    library("podcasts", isBook = false)
    item("podcasts", "podcast", "Podcast", isBook = false)
    val cached =
      listOf("unavailable", "saved").map { id ->
        PodcastEpisode(
          libraryItemId = "podcast",
          id = id,
          title = id,
          audioTrack = AudioTrack(metadata = FileMetadata(filename = "$id.mp3")),
        )
      }
    episodes.replace("podcast", cached)
    runBlocking {
      `when`(downloads.isPodcastEpisodeDownloaded("Podcast", "unavailable.mp3"))
        .thenThrow(SecurityException("Unable to read MediaStore"))
      `when`(downloads.isPodcastEpisodeDownloaded("Podcast", "saved.mp3")).thenReturn(true)
    }

    val statuses = children("car:podcast:podcast").associate { it.mediaId to it.downloadStatus() }
    assertEquals(notDownloaded, statuses["car:episode:podcast:unavailable"])
    assertEquals(
      MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED,
      statuses["car:episode:podcast:saved"],
    )
    assertEquals(notDownloaded, getItem("car:episode:podcast:unavailable").value?.downloadStatus())
  }

  @Test
  fun directLookupRefreshesMediaStoreAfterAnUnsubscribedRemoval() {
    val context = RuntimeEnvironment.getApplication()
    val storagePolicy = ReadableStoragePolicy()
    val audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val provider = EpisodeMediaProvider(storagePolicy.podcastRelativePath("Podcast"))
    ShadowContentResolver.registerProviderInternal(audioUri.authority, provider)

    withCatalog(downloadRepoForMediaStore(context, storagePolicy)) {
      library("podcasts", isBook = false)
      item("podcasts", "podcast", "Podcast", isBook = false)
      episodes.replace(
        "podcast",
        listOf(
          PodcastEpisode(
            libraryItemId = "podcast",
            id = "saved",
            title = "Saved",
            audioTrack = AudioTrack(metadata = FileMetadata(filename = "saved.mp3")),
          )
        ),
      )
      val episodeId = "car:episode:podcast:saved"
      assertEquals(
        MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED,
        getItem(episodeId).value?.downloadStatus(),
      )
      assertEquals(1, context.contentResolver.delete(audioUri, null, null))
      assertEquals(
        MediaConstants.EXTRAS_VALUE_STATUS_NOT_DOWNLOADED,
        getItem(episodeId).value?.downloadStatus(),
      )
      assertTrue(children("car:downloaded-episodes").isEmpty())
    }
  }

  @SuppressLint("UnsafeOptInUsageError")
  private fun downloadRepoForMediaStore(context: Context, storagePolicy: ReadableStoragePolicy) =
    DownloadRepo(
      mock(DownloadManager::class.java),
      mock(Helper::class.java),
      mock(DownloadMapper::class.java),
      BookDurableDownloadCatalog(context, storagePolicy, Dispatchers.IO),
      PodcastDurableDownloadCatalog(context, storagePolicy, Dispatchers.IO),
      mock(PlayPauseControlStateHolder::class.java),
      mock(PlayerInternalStateHolder::class.java),
      context,
    )

  private class EpisodeMediaProvider(private val relativePath: String) : ContentProvider() {
    private var hasEpisode = true

    override fun onCreate() = true

    override fun query(
      uri: Uri,
      projection: Array<out String>?,
      selection: String?,
      selectionArgs: Array<out String>?,
      sortOrder: String?,
    ): MatrixCursor =
      MatrixCursor(requireNotNull(projection)).apply {
        if (hasEpisode) addRow(arrayOf<Any>(1L, relativePath, "saved.mp3"))
      }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
      if (!hasEpisode) return 0
      hasEpisode = false
      return 1
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
      uri: Uri,
      values: ContentValues?,
      selection: String?,
      selectionArgs: Array<out String>?,
    ) = 0
  }

  private fun MediaItem.downloadStatus(): Long =
    requireNotNull(mediaMetadata.extras).getLong(MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS, -1)

  private class BrowseCallbackFixture(scope: CoroutineScope) {
    private val context = RuntimeEnvironment.getApplication()
    val repository = mock(CarCatalogRepository::class.java)
    val mapper = CarMediaItemMapper(context)
    val session = mock(MediaLibrarySession::class.java)
    val browser = mock(MediaSession.ControllerInfo::class.java)
    val callback =
      ShelfMediaLibraryCallback(
          context,
          Lazy<PlayerStore> { error("Browse does not need the player store") },
          Lazy { repository },
          mapper,
        )
        .also { it.attach(scope) }

    fun children(id: String) = callback.onGetChildren(session, browser, id, 0, Int.MAX_VALUE, null)
  }

  private fun withCatalog(downloadRepo: DownloadRepo? = null, test: Fixture.() -> Unit) {
    Fixture(downloadRepo).use(test)
  }

  private class Fixture(downloadRepo: DownloadRepo?) : AutoCloseable {
    val context = RuntimeEnvironment.getApplication()
    private val driver = DatabaseModule.provideSqlDriver(context)
    val db = DatabaseModule.provideSqlDelightAppDatabase(driver, DatabaseModule.provideMapAdapter())
    val episodes = PodcastEpisodeRepository(db, Json)
    val downloadChanges = MutableStateFlow(0)
    val downloads =
      downloadRepo
        ?: mock(DownloadRepo::class.java).also {
          `when`(it.durableDownloads).thenReturn(downloadChanges)
          runBlocking {
            `when`(it.podcastDownloadedEpisodeIds(anyString(), anyList())).thenReturn(emptySet())
          }
        }
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
        downloads,
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
