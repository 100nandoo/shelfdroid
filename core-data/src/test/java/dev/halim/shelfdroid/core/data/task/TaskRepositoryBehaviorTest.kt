package dev.halim.shelfdroid.core.data.task

import dev.halim.core.network.response.ServerTask as NetworkServerTask
import dev.halim.core.network.response.TasksResponse
import dev.halim.socketio.SocketEvent
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryBehaviorTest {

  @Test
  fun refreshReconcilesActiveSnapshot_butRetainsTerminalRows() = runTest {
    val active = networkTask("active", finished = false)
    val terminal = networkTask("terminal", finished = true, finishedAt = 100L)
    val api = FakeTaskApi(TasksResponse(listOf(active, terminal)), TasksResponse(emptyList()))
    val repository = repository(api)

    repository.refresh()
    assertEquals(setOf("active", "terminal"), repository.state.value.tasks.map { it.id }.toSet())

    repository.refresh()
    assertNull(repository.state.value.tasks.firstOrNull { it.id == "active" })
    assertNotNull(repository.state.value.tasks.firstOrNull { it.id == "terminal" })
  }

  @Test
  fun realTaskStarted_replacesAcceptedPlaceholder() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    repository.startLibraryScan("books")
    val placeholder = repository.state.value.tasks.single()
    assertTrue(placeholder.id.startsWith("accepted-scan-"))

    socket.emit("task_started", networkTask("real", finished = false).json())

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
    assertFalse(repository.state.value.tasks.any { it.id == placeholder.id })
  }

  @Test
  fun matchTaskStarted_replacesAcceptedPlaceholderAndUsesMatchAction() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    repository.startLibraryMatch("books")
    val placeholder = repository.state.value.tasks.single()
    assertTrue(placeholder.id.startsWith("accepted-match-"))
    assertEquals(TaskAction.BookMatching, placeholder.action)

    socket.emit(
      "task_started",
      networkTask("match", finished = false, action = "library-match-all").json(),
    )

    assertEquals(listOf("match"), repository.state.value.tasks.map { it.id })
    assertEquals(TaskAction.BookMatching, repository.state.value.tasks.single().action)
  }

  @Test
  fun scanAndMatchAreMutuallyExclusivePerLibraryButIndependentAcrossLibraries() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    assertTrue(repository.startLibraryScan("books").isSuccess)
    assertTrue(repository.startLibraryMatch("books").isFailure)
    assertTrue(repository.startLibraryMatch("podcasts").isSuccess)
    assertEquals(1, api.scanCalls)
    assertEquals(1, api.matchCalls)
  }

  @Test
  fun taskFinishedBeforeHttpResponse_doesNotLeavePlaceholder() = runTest {
    val scanResponse = CompletableDeferred<Result<Unit>>()
    val api = FakeTaskApi(TasksResponse(emptyList()), scanResponse)
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    val request = async { repository.startLibraryScan("books") }
    api.scanStarted.await()
    socket.emit("task_finished", networkTask("real", finished = true, finishedAt = 1_000L).json())
    scanResponse.complete(Result.success(Unit))
    request.await()
    runCurrent()

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
    assertFalse(repository.state.value.tasks.any { it.id.startsWith("accepted-scan-") })
  }

  @Test
  fun taskFinishedBeforeHttpResponseWithoutTimestamp_doesNotLeavePlaceholder() = runTest {
    val scanResponse = CompletableDeferred<Result<Unit>>()
    val api = FakeTaskApi(TasksResponse(emptyList()), scanResponse)
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    val request = async { repository.startLibraryScan("books") }
    api.scanStarted.await()
    socket.emit("task_finished", networkTask("real", finished = true).json())
    scanResponse.complete(Result.success(Unit))
    request.await()
    runCurrent()

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
  }

  @Test
  fun taskStartedBeforeHttpResponse_remainsActiveWithoutPlaceholder() = runTest {
    val scanResponse = CompletableDeferred<Result<Unit>>()
    val api = FakeTaskApi(TasksResponse(emptyList()), scanResponse)
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    val request = async { repository.startLibraryScan("books") }
    api.scanStarted.await()
    socket.emit("task_started", networkTask("real", finished = false).json())
    scanResponse.complete(Result.success(Unit))
    request.await()
    runCurrent()

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
    assertEquals(TaskStatus.ACTIVE, repository.state.value.tasks.single().status)
    assertFalse(repository.state.value.tasks.any { it.id.startsWith("accepted-scan-") })
  }

  @Test
  fun terminalTaskInRefreshSnapshot_reconcilesAcceptedPlaceholder() = runTest {
    val terminal = networkTask("real", finished = true, finishedAt = 1_000L)
    val repository =
      repository(
        FakeTaskApi(TasksResponse(listOf(terminal))),
        retentionMillis = 1_000_000L,
      )

    repository.startLibraryScan("books")

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
    assertFalse(repository.state.value.tasks.any { it.id.startsWith("accepted-scan-") })
  }

  @Test
  fun acceptedScan_survivesImmediateRecoveryAndLaterExplicitRefresh() = runTest {
    val api =
      FakeTaskApi(
        TasksResponse(emptyList()),
        TasksResponse(emptyList()),
      )
    val repository = repository(api, retentionMillis = 1_000_000L)

    repository.startLibraryScan("books")
    assertTrue(repository.state.value.tasks.single().id.startsWith("accepted-scan-"))

    repository.refresh()

    assertEquals(2, api.taskRequests)
    assertTrue(repository.state.value.snapshotKnown)
    assertTrue(repository.state.value.tasks.single().id.startsWith("accepted-scan-"))
  }

  @Test
  fun acceptedScan_reconcilesTimestampLessActiveSnapshotWithoutDuplicate() = runTest {
    val api =
      FakeTaskApi(
        TasksResponse(emptyList()),
        TasksResponse(listOf(networkTask("real", finished = false, startedAt = null))),
      )
    val repository = repository(api, retentionMillis = 1_000_000L)

    repository.startLibraryScan("books")
    assertTrue(repository.state.value.tasks.single().id.startsWith("accepted-scan-"))

    repository.refresh()

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
    assertFalse(repository.state.value.tasks.any { it.id.startsWith("accepted-scan-") })
  }

  @Test
  fun acceptedScan_survivesImmediateRecoveryAndReconnectRefresh() = runTest {
    val api =
      FakeTaskApi(
        TasksResponse(emptyList()),
        TasksResponse(emptyList()),
      )
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    repository.startLibraryScan("books")
    assertTrue(repository.state.value.tasks.single().id.startsWith("accepted-scan-"))

    socket.emit("connect")
    advanceUntilIdle()

    assertEquals(2, api.taskRequests)
    assertEquals(TaskConnectionState.CONNECTED, repository.state.value.connectionState)
    assertTrue(repository.state.value.snapshotKnown)
    assertTrue(repository.state.value.tasks.single().id.startsWith("accepted-scan-"))
  }

  @Test
  fun acceptedSocketTask_survivesRefreshUntilServerSnapshotContainsIt() = runTest {
    val api =
      FakeTaskApi(
        TasksResponse(emptyList()),
        TasksResponse(emptyList()),
      )
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1_000_000L)

    repository.startLibraryScan("books")
    socket.emit("task_started", networkTask("real", finished = false).json())

    repository.refresh()

    assertEquals(listOf("real"), repository.state.value.tasks.map { it.id })
    assertEquals(TaskStatus.ACTIVE, repository.state.value.tasks.single().status)
  }

  @Test
  fun reconnectAndExplicitRefreshAreTheOnlySnapshotRequests() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val socket = FakeTaskSocket()
    val repository = repository(api, socket)

    assertEquals(0, api.taskRequests)
    repository.refresh()
    assertEquals(1, api.taskRequests)
    runCurrent()
    assertEquals(1, api.taskRequests)

    socket.emit("connect")
    advanceUntilIdle()
    assertEquals(2, api.taskRequests)

    repository.refresh()
    assertEquals(3, api.taskRequests)
    advanceUntilIdle()
    assertEquals(3, api.taskRequests)
  }

  @Test
  fun terminalRowsExpireAfterRetentionWindow() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, retentionMillis = 1L)

    socket.emit("task_finished", networkTask("finished", finished = true, finishedAt = 0L).json())
    advanceUntilIdle()

    assertTrue(repository.state.value.tasks.isEmpty())
  }

  @Test
  fun staleActiveSnapshotCannotReopenCompletedTaskOrRepeatSynchronization() = runTest {
    val catalog = FakeCatalogSynchronizer(ArrayDeque(listOf(Result.success(Unit))))
    val api =
      FakeTaskApi(
        TasksResponse(emptyList()),
        TasksResponse(listOf(networkTask("finished", finished = false))),
      )
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, catalog = catalog, retentionMillis = 1_000_000L)

    repository.startLibraryScan("books")
    socket.emit(
      "task_finished",
      networkTask("finished", finished = true, finishedAt = 1_000L).json(),
    )
    runCurrent()
    repository.refresh()
    runCurrent()

    assertEquals(TaskStatus.COMPLETED, repository.state.value.tasks.single().status)
    assertEquals(TaskSyncState.SUCCEEDED, repository.state.value.tasks.single().syncState)
    assertEquals(1, catalog.synchronizationCalls)
  }

  @Test
  fun duplicateTerminalEventsScheduleOneNotificationAndOneSynchronization() = runTest {
    val catalog = FakeCatalogSynchronizer(ArrayDeque(listOf(Result.success(Unit))))
    val socket = FakeTaskSocket()
    val repository =
      repository(
        FakeTaskApi(TasksResponse(emptyList())),
        socket,
        catalog = catalog,
        retentionMillis = 1_000_000L,
      )
    val finished = networkTask("finished", finished = true, finishedAt = 1_000L).json()

    socket.emit("task_finished", finished)
    socket.emit("task_finished", finished)
    runCurrent()

    assertEquals(1, catalog.synchronizationCalls)
    assertEquals(
      TaskNotification("finished", TaskStatus.COMPLETED, TaskAction.LibraryScan),
      repository.notifications.value,
    )
  }

  @Test
  fun completedTaskSyncFailureIsDistinctAndRetryable() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val catalog =
      FakeCatalogSynchronizer(
        ArrayDeque(listOf(Result.failure(IllegalStateException("internal")), Result.success(Unit)))
      )
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, catalog = catalog, retentionMillis = 1_000_000L)

    socket.emit(
      "task_finished",
      networkTask("finished", finished = true, finishedAt = 1_000L).json(),
    )
    runCurrent()
    val failed = repository.state.value.tasks.single()
    assertEquals(TaskSyncState.FAILED, failed.syncState)
    assertEquals(TaskError.Generic, failed.syncError)

    assertTrue(repository.retrySynchronization("finished").isSuccess)
    runCurrent()
    assertEquals(TaskSyncState.SUCCEEDED, repository.state.value.tasks.single().syncState)
    assertEquals(0, api.scanCalls)
  }

  @Test
  fun completedMatchSyncFailureIsDistinctAndRetryDoesNotStartAnotherMatch() = runTest {
    val api = FakeTaskApi(TasksResponse(emptyList()))
    val catalog =
      FakeCatalogSynchronizer(
        ArrayDeque(listOf(Result.failure(IllegalStateException("internal")), Result.success(Unit)))
      )
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, catalog = catalog, retentionMillis = 1_000_000L)

    socket.emit(
      "task_finished",
      networkTask("match", finished = true, finishedAt = 1_000L, action = "library-match-all")
        .json(),
    )
    runCurrent()
    assertEquals(TaskSyncState.FAILED, repository.state.value.tasks.single().syncState)
    assertTrue(repository.retrySynchronization("match").isSuccess)
    runCurrent()
    assertEquals(TaskSyncState.SUCCEEDED, repository.state.value.tasks.single().syncState)
    assertEquals(0, api.matchCalls)
  }

  @Test
  fun refreshPreservesTerminalSynchronizationFailureForRetry() = runTest {
    val terminal = networkTask("finished", finished = true, finishedAt = 1_000L)
    val api = FakeTaskApi(TasksResponse(listOf(terminal)))
    val catalog =
      FakeCatalogSynchronizer(ArrayDeque(listOf(Result.failure(IllegalStateException("internal")))))
    val socket = FakeTaskSocket()
    val repository = repository(api, socket, catalog = catalog, retentionMillis = 1_000_000L)

    socket.emit("task_finished", terminal.json())
    runCurrent()
    assertTrue(repository.refresh().isSuccess)

    val refreshed = repository.state.value.tasks.single()
    assertEquals(TaskSyncState.FAILED, refreshed.syncState)
    assertEquals(TaskError.Generic, refreshed.syncError)
  }

  @Test
  fun terminalNotificationIsDurableDeduplicatedAndAcknowledged() = runTest {
    val repository = repository(FakeTaskApi(TasksResponse(emptyList())))
    val finished = networkTask("finished", finished = true, finishedAt = 1_000L).json()

    // No collector is attached: StateFlow retains the pending notification for a later screen.
    repository.socketForTest.emit("task_finished", finished)
    assertEquals(
      TaskNotification("finished", TaskStatus.COMPLETED, TaskAction.LibraryScan),
      repository.notifications.value,
    )
    repository.socketForTest.emit("task_finished", finished)
    assertEquals(
      TaskNotification("finished", TaskStatus.COMPLETED, TaskAction.LibraryScan),
      repository.notifications.value,
    )

    repository.acknowledgeNotification("finished")
    assertNull(repository.notifications.value)
  }

  @Test
  fun matchNotificationRetainsOperationForDelayedSnackbarPresentation() = runTest {
    val repository = repository(FakeTaskApi(TasksResponse(emptyList())))

    repository.socketForTest.emit(
      "task_finished",
      networkTask("match", finished = true, finishedAt = 1_000L, action = "library-match-all")
        .json(),
    )

    assertEquals(
      TaskNotification("match", TaskStatus.COMPLETED, TaskAction.BookMatching),
      repository.notifications.value,
    )
  }

  @Test
  fun unknownTaskNotificationRetainsRawOperationForGenericPresentation() = runTest {
    val repository = repository(FakeTaskApi(TasksResponse(emptyList())))

    repository.socketForTest.emit(
      "task_finished",
      networkTask(
          "future",
          finished = true,
          finishedAt = 1_000L,
          action = "future-server-task",
        )
        .json(),
    )

    assertEquals(
      TaskNotification(
        "future",
        TaskStatus.COMPLETED,
        TaskAction.Unknown("future-server-task"),
      ),
      repository.notifications.value,
    )
  }

  @Test
  fun refreshRecoversCompletedMatchAndSynchronizesCatalogWhenSocketEventWasMissed() = runTest {
    val api =
      FakeTaskApi(
        TasksResponse(
          listOf(
            networkTask("match", finished = true, finishedAt = 1_000L, action = "library-match-all")
          )
        )
      )
    val catalog = FakeCatalogSynchronizer(ArrayDeque(listOf(Result.success(Unit))))
    val repository = repository(api, catalog = catalog, retentionMillis = 1_000_000L)

    assertTrue(repository.refresh().isSuccess)
    runCurrent()

    assertEquals(TaskSyncState.SUCCEEDED, repository.state.value.tasks.single().syncState)
    assertEquals(
      TaskNotification("match", TaskStatus.COMPLETED, TaskAction.BookMatching),
      repository.notifications.value,
    )
  }

  private fun TestScope.repository(
    api: FakeTaskApi,
    socket: FakeTaskSocket = FakeTaskSocket(),
    catalog: FakeCatalogSynchronizer = FakeCatalogSynchronizer(),
    retentionMillis: Long = TaskRepository.TERMINAL_RETENTION_MILLIS,
  ): TestRepository {
    val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
    val repository =
      TaskRepository.forTest(
        api = api,
        socket = socket,
        catalogSynchronizer = catalog,
        scope = scope,
        json =
          Json {
            ignoreUnknownKeys = true
            explicitNulls = false
          },
        clock = TaskClock { 1_000L },
        terminalRetentionMillis = retentionMillis,
      )
    return TestRepository(repository, socket)
  }

  private data class TestRepository(
    val repository: TaskRepository,
    val socketForTest: FakeTaskSocket,
  ) : TaskRepositoryContract by repository

  private fun networkTask(
    id: String,
    finished: Boolean,
    finishedAt: Long? = null,
    startedAt: Long? = 0L,
    action: String = "library-scan",
    libraryId: String = "books",
  ): NetworkServerTask =
    NetworkServerTask(
      id = id,
      action = action,
      data = buildJsonObject { put("libraryId", libraryId) },
      isFinished = finished,
      startedAt = startedAt,
      finishedAt = finishedAt,
    )

  private fun NetworkServerTask.json(): String = Json {
    explicitNulls = false
  }
    .encodeToString(NetworkServerTask.serializer(), this)

  private class FakeTaskApi : TaskApi {
    private val snapshots: ArrayDeque<TasksResponse>
    private val scanResponse: CompletableDeferred<Result<Unit>>?
    var taskRequests = 0
    var scanCalls = 0
    var matchCalls = 0
    val scanStarted = CompletableDeferred<Unit>()

    constructor(vararg snapshots: TasksResponse) {
      this.snapshots = ArrayDeque(snapshots.toList())
      scanResponse = null
    }

    constructor(snapshot: TasksResponse, scanResponse: CompletableDeferred<Result<Unit>>) {
      this.snapshots = ArrayDeque(listOf(snapshot))
      this.scanResponse = scanResponse
    }

    override suspend fun tasks(): Result<TasksResponse> {
      taskRequests++
      return Result.success(
        if (snapshots.isEmpty()) TasksResponse(emptyList()) else snapshots.removeFirst()
      )
    }

    override suspend fun scanLibrary(libraryId: String): Result<Unit> {
      scanCalls++
      scanStarted.complete(Unit)
      return scanResponse?.await() ?: Result.success(Unit)
    }

    override suspend fun matchLibrary(libraryId: String): Result<Unit> {
      matchCalls++
      return Result.success(Unit)
    }
  }

  private class FakeCatalogSynchronizer(
    private val responses: ArrayDeque<Result<Unit>> = ArrayDeque()
  ) : TaskCatalogSynchronizer {
    var synchronizationCalls = 0

    override suspend fun synchronize(): Result<Unit> =
      if (responses.isEmpty()) {
        synchronizationCalls++
        Result.success(Unit)
      } else {
        synchronizationCalls++
        responses.removeFirst()
      }
  }

  private class FakeTaskSocket : TaskSocket {
    private val listeners = mutableMapOf<String, MutableList<(Array<Any>) -> Unit>>()

    override fun acquire(): AutoCloseable = AutoCloseable {}

    override fun subscribe(event: SocketEvent, listener: (Array<Any>) -> Unit): AutoCloseable {
      val eventListeners = listeners.getOrPut(event.name) { mutableListOf() }
      eventListeners += listener
      return AutoCloseable { eventListeners.remove(listener) }
    }

    fun emit(event: String, vararg args: Any) {
      listeners[event].orEmpty().toList().forEach { it(arrayOf(*args)) }
    }
  }
}
