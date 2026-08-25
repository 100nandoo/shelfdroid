package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.shelfdroid.core.data.task.ServerTaskSocket
import dev.halim.socketio.SocketManager.Event.Library as LibrarySocketEvent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryAdministrationEventOwnerTest {
  @Test
  fun ownerSharesSocketOwnershipAndReconcilesEventsWithoutPolling() = runTest {
    val socket = FakeServerTaskSocket()
    val taskOwner = socket.acquire()
    val podcastOwner = socket.acquire()
    var taskCalls = 0
    var podcastCalls = 0
    val taskSubscription = socket.subscribe("task_started") { taskCalls++ }
    val podcastSubscription = socket.subscribe("episode_download_started") { podcastCalls++ }
    var synchronizations = 0
    val currentLibraries =
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
          synchronizations++
          Result.success(Unit)
        },
        removeLibraryItems = {},
        removeLibrary = {},
        currentLibraries = { currentLibraries },
      )
    val published = mutableListOf<LibraryAdministrationLibraryEvent>()
    val owner =
      LibraryAdministrationEventOwner(
        socket = socket,
        json = Json { ignoreUnknownKeys = true },
        scope = this,
        reconciler = reconciler,
      ) { event ->
        reconciler.reconcile(event).onSuccess { libraries ->
          published += event.copy(libraries = libraries)
        }
      }

    assertEquals(3, socket.activeOwners)
    socket.emit("task_started", "task")
    socket.emit("episode_download_started", "podcast")
    assertEquals(1, taskCalls)
    assertEquals(1, podcastCalls)

    // A connection recovery performs one authoritative refresh.
    socket.emit("connect")
    advanceUntilIdle()
    assertEquals(1, synchronizations)
    assertEquals(LibraryAdministrationLibraryEventType.REFRESHED, published.single().type)

    // Ordinary elapsed time does not start a polling refresh.
    advanceTimeBy(10_000)
    advanceUntilIdle()
    assertEquals(1, synchronizations)

    val payload =
      """{"id":"books","name":"Books","mediaType":"book","displayOrder":0}"""
    socket.emit(LibrarySocketEvent.ADDED, payload)
    advanceUntilIdle()
    assertEquals(2, synchronizations)
    socket.emit(LibrarySocketEvent.ADDED, payload)
    advanceUntilIdle()
    assertEquals(2, synchronizations)

    socket.emit(
      LibrarySocketEvent.UPDATED,
      """{"id":"books","name":"Renamed","mediaType":"book","displayOrder":0}""",
    )
    socket.emit(
      LibrarySocketEvent.REMOVED,
      """{"id":"books","name":"Renamed","mediaType":"book","displayOrder":0}""",
    )
    advanceUntilIdle()
    assertEquals(4, synchronizations)
    assertEquals(
      listOf(
        LibraryAdministrationLibraryEventType.REFRESHED,
        LibraryAdministrationLibraryEventType.ADDED,
        LibraryAdministrationLibraryEventType.UPDATED,
        LibraryAdministrationLibraryEventType.REMOVED,
      ),
      published.map { it.type },
    )

    // Closing the Library owner releases only its handle; task/podcast consumers remain alive.
    owner.close()
    assertEquals(2, socket.activeOwners)
    socket.emit("task_started", "task-again")
    socket.emit("episode_download_started", "podcast-again")
    assertEquals(2, taskCalls)
    assertEquals(2, podcastCalls)
    assertTrue(socket.activeOwners > 0)

    taskSubscription.close()
    podcastSubscription.close()
    taskOwner.close()
    podcastOwner.close()
    assertEquals(0, socket.activeOwners)
  }

  private class FakeServerTaskSocket : ServerTaskSocket {
    private val listeners = mutableMapOf<String, MutableList<(Array<Any>) -> Unit>>()
    var activeOwners: Int = 0
      private set

    override fun acquire(): AutoCloseable {
      activeOwners++
      var closed = false
      return AutoCloseable {
        if (!closed) {
          closed = true
          activeOwners--
        }
      }
    }

    override fun subscribe(event: String, listener: (Array<Any>) -> Unit): AutoCloseable {
      listeners.getOrPut(event) { mutableListOf() }.add(listener)
      var closed = false
      return AutoCloseable {
        if (!closed) {
          closed = true
          listeners[event]?.remove(listener)
        }
      }
    }

    fun emit(event: String, payload: Any = Unit) {
      listeners[event]?.toList()?.forEach { listener -> listener(arrayOf(payload)) }
    }
  }
}
