package dev.halim.shelfdroid.core.data.catalog

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogSynchronizerTest {

  @Test
  fun synchronize_whenLibrariesFail_doesNotRefreshItems() = runTest {
    val failure = IllegalStateException("libraries unavailable")
    var itemRefreshes = 0
    val synchronizer =
      CatalogSynchronizer(
        refreshLibraries = { Result.failure(failure) },
        refreshLibraryItems = {
          itemRefreshes++
          LibraryItemRefreshResult(emptySet(), emptyList())
        },
        scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
      )

    val result = synchronizer.synchronize()

    assertFalse(result.isSuccess)
    assertSame(failure, result.libraries.exceptionOrNull())
    assertNull(result.items)
    assertEquals(0, itemRefreshes)
  }

  @Test
  fun synchronize_reportsPartialLibraryItemRefreshes() = runTest {
    val failure = IllegalStateException("library unavailable")
    val items =
      LibraryItemRefreshResult(
        refreshedLibraryIds = setOf("books"),
        failures = listOf(LibraryItemRefreshFailure("podcasts", failure)),
      )
    val synchronizer =
      CatalogSynchronizer(
        refreshLibraries = { Result.success(Unit) },
        refreshLibraryItems = { items },
        scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
      )

    val result = synchronizer.synchronize()

    assertFalse(result.isSuccess)
    assertEquals(items, result.items)
    assertSame(failure, result.error)
  }

  @Test
  fun synchronize_whenRefreshThrows_returnsFailureResult() = runTest {
    val failure = IllegalStateException("unexpected failure")
    val synchronizer =
      CatalogSynchronizer(
        refreshLibraries = { throw failure },
        refreshLibraryItems = { error("items should not refresh") },
        scope = CoroutineScope(StandardTestDispatcher(testScheduler)),
      )

    val result = synchronizer.synchronize()

    assertFalse(result.isSuccess)
    assertSame(failure, result.error)
  }

  @Test
  fun synchronize_sharesAnInFlightRefresh() = runTest {
    val release = CompletableDeferred<Unit>()
    val libraryRefreshes = AtomicInteger(0)
    val dispatcher = StandardTestDispatcher(testScheduler)
    val synchronizer =
      CatalogSynchronizer(
        refreshLibraries = {
          libraryRefreshes.incrementAndGet()
          release.await()
          Result.success(Unit)
        },
        refreshLibraryItems = {
          LibraryItemRefreshResult(emptySet(), emptyList())
        },
        scope = CoroutineScope(dispatcher),
      )

    val first = async { synchronizer.synchronize() }
    runCurrent()
    val second = async { synchronizer.synchronize() }
    runCurrent()

    assertEquals(1, libraryRefreshes.get())
    assertFalse(first.isCompleted)
    assertFalse(second.isCompleted)

    release.complete(Unit)
    val firstResult = first.await()
    val secondResult = second.await()

    assertTrue(firstResult.isSuccess)
    assertTrue(secondResult.isSuccess)
    assertEquals(1, libraryRefreshes.get())
  }
}
