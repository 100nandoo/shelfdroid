package dev.halim.shelfdroid.core.data.screen.libraryadministration

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdministrationLibraryEventTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun parserMapsServerLibraryEventsToCatalogProjection() {
    val event =
      parseLibraryAdministrationLibraryEvent(
        eventName = "library_added",
        args =
          arrayOf(
            """
            {"id":"books","name":"Books","mediaType":"book","displayOrder":3,
             "settings":{"autoScanCronExpression":"0 * * * *"},"serverOnly":"ignored"}
            """.trimIndent()
          ),
        json = json,
      )

    assertEquals(LibraryAdministrationLibraryEventType.ADDED, event?.type)
    assertEquals(
      LibraryAdministrationLibrary(
        id = "books",
        name = "Books",
        mediaType = LibraryAdministrationMediaType.BOOK,
        displayOrder = 3,
      ),
      event?.library,
    )
    assertTrue(event!!.fingerprint != 0)
  }

  @Test
  fun parserMapsUpdatedAndRemovedEvents() {
    val updated =
      parseLibraryAdministrationLibraryEvent(
        "library_updated",
        arrayOf("""{"id":"books","name":"Renamed","mediaType":"book","displayOrder":2}"""),
        json,
      )
    val removed =
      parseLibraryAdministrationLibraryEvent(
        "library_removed",
        arrayOf("""{"id":"books","name":"Renamed","mediaType":"book","displayOrder":2}"""),
        json,
      )

    assertEquals(LibraryAdministrationLibraryEventType.UPDATED, updated?.type)
    assertEquals(LibraryAdministrationLibraryEventType.REMOVED, removed?.type)
    assertEquals("Renamed", updated?.library?.name)
  }

  @Test
  fun parserRejectsUnknownEventsAndInvalidPayloads() {
    assertEquals(
      null,
      parseLibraryAdministrationLibraryEvent("library_changed", arrayOf("{}"), json),
    )
    assertEquals(
      null,
      parseLibraryAdministrationLibraryEvent("library_added", arrayOf("not-json"), json),
    )
    assertEquals(
      null,
      parseLibraryAdministrationLibraryEvent(
        "library_removed",
        arrayOf("""{"name":"Missing id"}"""),
        json,
      ),
    )
  }

  @Test
  fun richAdministrationChangesProduceDifferentDeduplicationKeys() {
    val first =
      parseLibraryAdministrationLibraryEvent(
        "library_updated",
        arrayOf(
          """{"id":"books","name":"Books","mediaType":"book","settings":{"disableWatcher":false}}"""
        ),
        json,
      )!!
    val second =
      parseLibraryAdministrationLibraryEvent(
        "library_updated",
        arrayOf(
          """{"id":"books","name":"Books","mediaType":"book","settings":{"disableWatcher":true}}"""
        ),
        json,
      )!!

    assertFalse(first.deduplicationKey() == second.deduplicationKey())
  }

  @Test
  fun reconcilerDeduplicatesEventsAndUsesServerSnapshot() = runTest {
    var synchronizeCalls = 0
    var libraries =
      listOf(
        LibraryAdministrationLibrary(
          id = "books",
          name = "Books",
          mediaType = LibraryAdministrationMediaType.BOOK,
          displayOrder = 0,
        )
      )
    val reconciler =
      LibraryAdministrationEventReconciler(
        mutationCoordinator = LibraryMutationCoordinator(),
        synchronize = {
          synchronizeCalls++
          libraries =
            listOf(
              LibraryAdministrationLibrary(
                id = "podcasts",
                name = "Podcasts",
                mediaType = LibraryAdministrationMediaType.PODCAST,
                displayOrder = 0,
              ),
              libraries.first(),
            )
          Result.success(Unit)
        },
        removeLibraryItems = {},
        removeLibrary = {},
        currentLibraries = { libraries },
      )
    val event =
      LibraryAdministrationLibraryEvent(
        type = LibraryAdministrationLibraryEventType.ADDED,
        library = libraries.first(),
        fingerprint = 11,
      )

    assertTrue(reconciler.accept(event))
    assertFalse(reconciler.accept(event))
    assertEquals(2, reconciler.reconcile(event).getOrThrow().size)
    assertEquals(1, synchronizeCalls)
  }

  @Test
  fun localMutationEcho_isConsumedWithoutASecondSynchronization() = runTest {
    var synchronizeCalls = 0
    val library =
      LibraryAdministrationLibrary(
        id = "books",
        name = "Books",
        mediaType = LibraryAdministrationMediaType.BOOK,
        displayOrder = 0,
      )
    val reconciler =
      LibraryAdministrationEventReconciler(
        mutationCoordinator = LibraryMutationCoordinator(),
        synchronize = {
          synchronizeCalls++
          Result.success(Unit)
        },
        removeLibraryItems = {},
        removeLibrary = {},
        currentLibraries = { listOf(library) },
      )
    val event =
      LibraryAdministrationLibraryEvent(
        type = LibraryAdministrationLibraryEventType.ADDED,
        library = library,
        fingerprint = 15,
      )

    reconciler.registerLocalMutation(event)
    assertFalse(reconciler.accept(event))
    assertEquals(0, synchronizeCalls)

    // A socket callback can win the race before the HTTP mutation returns. Registration while
    // the global mutation gate is held must still suppress the pending reconciliation.
    assertTrue(reconciler.accept(event.copy(fingerprint = 16)))
    reconciler.registerLocalMutation(event.copy(fingerprint = 16))
    assertEquals(listOf(library), reconciler.reconcile(event.copy(fingerprint = 16)).getOrThrow())
    assertEquals(0, synchronizeCalls)
  }

  @Test
  fun reconnectRefreshesAreNeverDeduplicated() {
    val reconciler =
      LibraryAdministrationEventReconciler(
        mutationCoordinator = LibraryMutationCoordinator(),
        synchronize = { Result.success(Unit) },
        removeLibraryItems = {},
        removeLibrary = {},
        currentLibraries = { emptyList() },
      )
    val refresh =
      LibraryAdministrationLibraryEvent(
        type = LibraryAdministrationLibraryEventType.REFRESHED,
      )

    assertTrue(reconciler.accept(refresh))
    assertTrue(reconciler.accept(refresh))
  }

  @Test
  fun removedEventCleansLocalCatalogBeforeSynchronization() = runTest {
    val operations = mutableListOf<String>()
    val library =
      LibraryAdministrationLibrary(
        id = "books",
        name = "Books",
        mediaType = LibraryAdministrationMediaType.BOOK,
        displayOrder = 0,
      )
    val reconciler =
      LibraryAdministrationEventReconciler(
        mutationCoordinator = LibraryMutationCoordinator(),
        synchronize = {
          operations += "sync"
          Result.success(Unit)
        },
        removeLibraryItems = {
          assertEquals("books", it)
          operations += "items"
        },
        removeLibrary = {
          assertEquals("books", it)
          operations += "library"
        },
        currentLibraries = { emptyList() },
      )
    val event =
      LibraryAdministrationLibraryEvent(
        type = LibraryAdministrationLibraryEventType.REMOVED,
        library = library,
        fingerprint = 12,
      )

    assertTrue(reconciler.accept(event))
    assertEquals(emptyList<LibraryAdministrationLibrary>(), reconciler.reconcile(event).getOrThrow())
    assertEquals(listOf("items", "library", "sync"), operations)
  }
}
