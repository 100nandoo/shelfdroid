package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.response.Library
import dev.halim.core.network.response.NetworkMediaType
import dev.halim.shelfdroid.core.MediaType
import dev.halim.socketio.SocketEvent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdminLibraryEventTest {
  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun serverLibraryProjectionPreservesAdministrationFields() {
    val library =
      Library(
        id = "books",
        name = "Books",
        mediaType = NetworkMediaType.BOOK,
        displayOrder = 4,
      )

    assertEquals(
      LibraryAdminLibrary(
        id = "books",
        name = "Books",
        mediaType = MediaType.BOOK,
        displayOrder = 4,
      ),
      library.toAdministrationLibrary(),
    )
  }

  @Test
  fun parserMapsServerLibraryEventsToCatalogProjection() {
    val event =
      parseLibraryAdminLibraryEvent(
        eventName = SocketEvent.Library.Added,
        args =
          arrayOf(
            """
            {"id":"books","name":"Books","mediaType":"book","displayOrder":3,
             "settings":{"autoScanCronExpression":"0 * * * *"},"serverOnly":"ignored"}
            """
              .trimIndent()
          ),
        json = json,
      )

    assertEquals(LibraryAdminLibraryEventType.ADDED, event?.type)
    assertEquals(
      LibraryAdminLibrary(
        id = "books",
        name = "Books",
        mediaType = MediaType.BOOK,
        displayOrder = 3,
      ),
      event?.library,
    )
    assertTrue(event!!.fingerprint != 0)
  }

  @Test
  fun parserMapsUpdatedAndRemovedEvents() {
    val updated =
      parseLibraryAdminLibraryEvent(
        SocketEvent.Library.Updated,
        arrayOf("""{"id":"books","name":"Renamed","mediaType":"book","displayOrder":2}"""),
        json,
      )
    val removed =
      parseLibraryAdminLibraryEvent(
        SocketEvent.Library.Removed,
        arrayOf("""{"id":"books","name":"Renamed","mediaType":"book","displayOrder":2}"""),
        json,
      )

    assertEquals(LibraryAdminLibraryEventType.UPDATED, updated?.type)
    assertEquals(LibraryAdminLibraryEventType.REMOVED, removed?.type)
    assertEquals("Renamed", updated?.library?.name)
  }

  @Test
  fun parserRejectsUnknownEventsAndInvalidPayloads() {
    assertEquals(
      null,
      parseLibraryAdminLibraryEvent(SocketEvent.Connect, arrayOf("{}"), json),
    )
    assertEquals(
      null,
      parseLibraryAdminLibraryEvent(
        SocketEvent.Library.Added,
        arrayOf("not-json"),
        json,
      ),
    )
    assertEquals(
      null,
      parseLibraryAdminLibraryEvent(
        SocketEvent.Library.Removed,
        arrayOf("""{"name":"Missing id"}"""),
        json,
      ),
    )
  }

  @Test
  fun richAdministrationChangesProduceDifferentDeduplicationKeys() {
    val first =
      parseLibraryAdminLibraryEvent(
        SocketEvent.Library.Updated,
        arrayOf(
          """{"id":"books","name":"Books","mediaType":"book","settings":{"disableWatcher":false}}"""
        ),
        json,
      )!!
    val second =
      parseLibraryAdminLibraryEvent(
        SocketEvent.Library.Updated,
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
        LibraryAdminLibrary(
          id = "books",
          name = "Books",
          mediaType = MediaType.BOOK,
          displayOrder = 0,
        )
      )
    val reconciler =
      LibraryAdminEventReconciler(
        mutationCoordinator = LibraryMutationCoordinator(),
        synchronize = {
          synchronizeCalls++
          libraries =
            listOf(
              LibraryAdminLibrary(
                id = "podcasts",
                name = "Podcasts",
                mediaType = MediaType.PODCAST,
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
      LibraryAdminLibraryEvent(
        type = LibraryAdminLibraryEventType.ADDED,
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
      LibraryAdminLibrary(
        id = "books",
        name = "Books",
        mediaType = MediaType.BOOK,
        displayOrder = 0,
      )
    val reconciler =
      LibraryAdminEventReconciler(
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
      LibraryAdminLibraryEvent(
        type = LibraryAdminLibraryEventType.ADDED,
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
      LibraryAdminEventReconciler(
        mutationCoordinator = LibraryMutationCoordinator(),
        synchronize = { Result.success(Unit) },
        removeLibraryItems = {},
        removeLibrary = {},
        currentLibraries = { emptyList() },
      )
    val refresh = LibraryAdminLibraryEvent(type = LibraryAdminLibraryEventType.REFRESHED)

    assertTrue(reconciler.accept(refresh))
    assertTrue(reconciler.accept(refresh))
  }

  @Test
  fun removedEventCleansLocalCatalogBeforeSynchronization() = runTest {
    val operations = mutableListOf<String>()
    val library =
      LibraryAdminLibrary(
        id = "books",
        name = "Books",
        mediaType = MediaType.BOOK,
        displayOrder = 0,
      )
    val reconciler =
      LibraryAdminEventReconciler(
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
      LibraryAdminLibraryEvent(
        type = LibraryAdminLibraryEventType.REMOVED,
        library = library,
        fingerprint = 12,
      )

    assertTrue(reconciler.accept(event))
    assertEquals(emptyList<LibraryAdminLibrary>(), reconciler.reconcile(event).getOrThrow())
    assertEquals(listOf("items", "library", "sync"), operations)
  }
}
