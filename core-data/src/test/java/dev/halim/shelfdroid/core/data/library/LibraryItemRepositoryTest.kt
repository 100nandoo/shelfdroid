package dev.halim.shelfdroid.core.data.library

import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.request.UpdateLibraryItemChaptersRequest
import dev.halim.core.network.response.UpdateLibraryItemChaptersResponse
import dev.halim.core.network.response.libraryitem.AudioFile
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryItemRepositoryTest {

  @Test
  fun primaryInoId_returnsFirstAudioFileIno() {
    val book = Book(audioFiles = listOf(AudioFile(ino = "ino-1"), AudioFile(ino = "ino-2")))

    assertEquals("ino-1", book.primaryInoId())
  }

  @Test
  fun primaryInoId_returnsEmptyStringWhenAudioFilesMissing() {
    val book = Book(audioFiles = emptyList())

    assertEquals("", book.primaryInoId())
  }

  @Test
  fun stalePodcastEpisodeIds_returnsRemovedEpisodeIdsFromIncomingItems() {
    val items =
      listOf(
        LibraryItem(
          id = "podcast-1",
          mediaType = "podcast",
          media = Podcast(episodes = listOf(PodcastEpisode(id = "episode-2"))),
        ),
        LibraryItem(
          id = "book-1",
          mediaType = "book",
          media = Book(),
        ),
      )

    val staleIds =
      stalePodcastEpisodeIds(
        existingEpisodeIds = setOf("episode-1", "episode-2", "episode-3"),
        items = items,
      )

    assertEquals(setOf("episode-1", "episode-3"), staleIds.toSet())
  }

  @Test
  fun fetchLibraryItemsInBatches_deduplicatesAndMergesChunks() = runTest {
    val requestedChunks = mutableListOf<List<String>>()
    val ids = (1..201).map { "item-$it" } + "item-1"

    val result =
      fetchLibraryItemsInBatches(ids) { chunk ->
        requestedChunks += chunk
        Result.success(emptyList())
      }

    assertTrue(result.isSuccess)
    assertEquals(5, requestedChunks.size)
    assertEquals(50, requestedChunks[0].size)
    assertEquals(50, requestedChunks[1].size)
    assertEquals(listOf("item-201"), requestedChunks[4])
  }

  @Test
  fun fetchLibraryItemsInBatches_runsChunksConcurrently() = runTest {
    val started = Channel<Unit>(Channel.UNLIMITED)
    val release = CompletableDeferred<Unit>()
    val result = async {
      fetchLibraryItemsInBatches((1..201).map { "item-$it" }) {
        started.send(Unit)
        release.await()
        Result.success(emptyList())
      }
    }

    started.receive()
    started.receive()
    assertFalse(result.isCompleted)

    release.complete(Unit)
    assertTrue(result.await().isSuccess)
  }

  @Test
  fun fetchLibraryItemsInBatches_doesNotReturnPartialResultsWhenAChunkFails() = runTest {
    val failure = IllegalStateException("chunk failed")

    val result =
      fetchLibraryItemsInBatches((1..101).map { "item-$it" }) { chunk ->
        if (chunk.first() == "item-101") Result.failure(failure) else Result.success(emptyList())
      }

    assertTrue(result.isFailure)
    assertSame(failure, result.exceptionOrNull())
  }

  @Test
  fun updateLibraryItemChaptersAndRefresh_doesNotRefreshAfterUpdateFailure() = runTest {
    val failure = IllegalStateException("update failed")
    var refreshed = false

    val result =
      updateLibraryItemChaptersAndRefresh(
        itemId = "book-1",
        request = UpdateLibraryItemChaptersRequest(chapters = emptyList()),
        update = { _, _ -> Result.failure(failure) },
        refresh = {
          refreshed = true
          Result.success(LibraryItem(id = "book-1", media = Book()))
        },
      )

    assertSame(failure, result.exceptionOrNull())
    assertFalse(refreshed)
  }

  @Test
  fun updateLibraryItemChaptersAndRefresh_returnsRefreshFailureAfterSuccessfulUpdate() = runTest {
    val failure = IllegalStateException("refresh failed")

    val result =
      updateLibraryItemChaptersAndRefresh(
        itemId = "book-1",
        request = UpdateLibraryItemChaptersRequest(chapters = emptyList()),
        update = { _, _ -> Result.success(UpdateLibraryItemChaptersResponse(success = true)) },
        refresh = { Result.failure(failure) },
      )

    assertSame(failure, result.exceptionOrNull())
  }

  @Test
  fun updateLibraryItemChaptersAndRefresh_returnsRefreshedItemAfterSuccessfulUpdate() = runTest {
    val refreshedItem = LibraryItem(id = "book-1", media = Book())

    val result =
      updateLibraryItemChaptersAndRefresh(
        itemId = "book-1",
        request = UpdateLibraryItemChaptersRequest(chapters = emptyList()),
        update = { _, _ -> Result.success(UpdateLibraryItemChaptersResponse(success = true)) },
        refresh = { Result.success(refreshedItem) },
      )

    assertSame(refreshedItem, result.getOrNull())
  }
}
