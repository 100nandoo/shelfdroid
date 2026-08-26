package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationConnectionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationTaskState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canDelete
import dev.halim.shelfdroid.core.data.task.ServerTask
import dev.halim.shelfdroid.core.data.task.ServerTaskConnectionState
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryAdministrationViewModelTest {

  @Test
  fun loadSuccess_preservesServerOrderAndIdentity() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val libraries =
      listOf(
        LibraryAdministrationLibrary(
          "podcasts",
          "Podcasts",
          LibraryAdministrationMediaType.PODCAST,
          4,
        ),
        LibraryAdministrationLibrary(
          "books",
          "Books",
          LibraryAdministrationMediaType.BOOK,
          9,
        ),
      )
    val viewModel = LibraryAdministrationViewModel(FakeRepository(listOf(Result.success(libraries))))
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(libraries, viewModel.uiState.value.libraries)
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    collection.cancel()
  }

  @Test
  fun loadFailure_doesNotExposeCachedNamesAndCanRetry() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        listOf(
          Result.failure(IllegalStateException("offline")),
          Result.success(
            listOf(
              LibraryAdministrationLibrary(
                "books",
                "Books",
                LibraryAdministrationMediaType.BOOK,
                0,
              )
            )
          ),
        ),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.state is GenericState.Failure)
    assertNull((viewModel.uiState.value.state as GenericState.Failure).errorMessage)
    assertTrue(viewModel.uiState.value.libraries.isEmpty())

    viewModel.onEvent(LibraryAdministrationEvent.Refresh)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(listOf("Books"), viewModel.uiState.value.libraries.map { it.name })
    assertEquals(2, repository.loadCalls)
    collection.cancel()
  }

  @Test
  fun explicitRefresh_reconcilesLibrariesAfterMissedEventsWithoutPolling() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts")
    val refreshed =
      listOf(
        LibraryAdministrationLibrary(
          id = "new-books",
          name = "New Books",
          mediaType = LibraryAdministrationMediaType.BOOK,
          displayOrder = 1,
        ),
        LibraryAdministrationLibrary(
          id = "books",
          name = "Renamed Books",
          mediaType = LibraryAdministrationMediaType.BOOK,
          displayOrder = 2,
        ),
      )
    val repository =
      FakeRepository(
        listOf(Result.success(initial), Result.success(refreshed)),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    // The server snapshot includes an added, updated, removed, and reordered result even though
    // no corresponding socket event was delivered to this screen.
    viewModel.onEvent(LibraryAdministrationEvent.Refresh)
    advanceUntilIdle()

    assertEquals(refreshed, viewModel.uiState.value.libraries)
    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(2, repository.loadCalls)

    advanceTimeBy(60_000)
    advanceUntilIdle()
    assertEquals(2, repository.loadCalls)
    collection.cancel()
  }

  @Test
  fun refreshFailure_keepsNamesHiddenByFailureState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        listOf(
          Result.success(
            listOf(
              LibraryAdministrationLibrary(
                "books",
                "Books",
                LibraryAdministrationMediaType.BOOK,
                0,
              )
            )
          ),
          Result.failure(IllegalStateException("offline")),
        ),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationEvent.Refresh)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.state is GenericState.Failure)
    assertEquals(listOf("Books"), viewModel.uiState.value.libraries.map { it.name })
    collection.cancel()
  }

  @Test
  fun moveLibrary_updatesOptimisticallyAndUsesServerAuthoritativeOrder() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts")
    val accepted = libraries("podcasts", "books")
    val repository = FakeRepository(listOf(Result.success(initial)), listOf(Result.success(accepted)))
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "books", "podcasts")

    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    advanceUntilIdle()

    assertEquals(accepted, viewModel.uiState.value.libraries)
    assertEquals(listOf(accepted), repository.reorderRequests.map { it.second })
    assertEquals(false, viewModel.uiState.value.isReordering)
    collection.cancel()
  }

  @Test
  fun failedMove_rollsBackAndExposesVisibleError() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts")
    val repository =
      FakeRepository(
        listOf(Result.success(initial)),
        listOf(Result.failure(IllegalStateException("offline"))),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "books", "podcasts")

    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    advanceUntilIdle()

    assertEquals(initial, viewModel.uiState.value.libraries)
    assertEquals("offline", viewModel.uiState.value.reorderError)
    collection.cancel()
  }

  @Test
  fun moveLibrary_isDisabledForUnknownActiveOrDisconnectedTaskState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts")
    val repository = FakeRepository(listOf(Result.success(initial)))
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(
      LibraryAdministrationEvent.SetConnectionState(
        LibraryAdministrationConnectionState.CONNECTED
      )
    )
    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)

    viewModel.onEvent(
      LibraryAdministrationEvent.SetTaskState("books", LibraryAdministrationTaskState.ACTIVE)
    )
    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)

    viewModel.onEvent(
      LibraryAdministrationEvent.SetTaskState("books", LibraryAdministrationTaskState.IDLE)
    )
    viewModel.onEvent(
      LibraryAdministrationEvent.SetTaskState("podcasts", LibraryAdministrationTaskState.ACTIVE)
    )
    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)

    viewModel.onEvent(
      LibraryAdministrationEvent.SetTaskState("podcasts", LibraryAdministrationTaskState.IDLE)
    )
    viewModel.onEvent(
      LibraryAdministrationEvent.SetConnectionState(LibraryAdministrationConnectionState.DISCONNECTED)
    )
    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)
    collection.cancel()
  }

  @Test
  fun newestCompletedReorderAcknowledgementWinsOverOlderIntent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts", "third")
    val firstAccepted = libraries("podcasts", "books", "third")
    val secondAccepted = libraries("podcasts", "third", "books")
    val first = CompletableDeferred<Result<List<LibraryAdministrationLibrary>>>()
    val second = CompletableDeferred<Result<List<LibraryAdministrationLibrary>>>()
    val repository =
      ControlledRepository(
        initial = initial,
        responses = ArrayDeque(listOf(first, second)),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "books", "podcasts", "third")

    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    viewModel.onEvent(LibraryAdministrationEvent.MoveLibrary("books", 1))
    second.complete(Result.success(secondAccepted))
    advanceUntilIdle()
    first.complete(Result.success(firstAccepted))
    advanceUntilIdle()

    assertEquals(secondAccepted, viewModel.uiState.value.libraries)
    collection.cancel()
  }

  @Test
  fun scanStartsOnlyWhenConnectionSnapshotAndLibraryTaskAreKnownIdle() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      TaskRepository(
        libraries = libraries("books"),
        initialTaskState =
          ServerTaskRepositoryState(
            connectionState = ServerTaskConnectionState.CONNECTED,
            snapshotKnown = true,
          ),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationEvent.StartScan("books"))
    advanceUntilIdle()
    assertEquals(listOf("books"), repository.scanRequests)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "scan",
              action = "library-scan",
              libraryId = "books",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationEvent.StartScan("books"))
    advanceUntilIdle()
    assertEquals(listOf("books"), repository.scanRequests)
    collection.cancel()
  }

  @Test
  fun matchStartsOnlyForBookLibrariesAndSharesPerLibraryTaskGateWithScan() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      TaskRepository(
        libraries =
          listOf(
            libraries("books").single(),
            LibraryAdministrationLibrary(
              "podcasts",
              "Podcasts",
              LibraryAdministrationMediaType.PODCAST,
              2,
            ),
          ),
        initialTaskState =
          ServerTaskRepositoryState(
            connectionState = ServerTaskConnectionState.CONNECTED,
            snapshotKnown = true,
          ),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationEvent.StartMatch("books"))
    viewModel.onEvent(LibraryAdministrationEvent.StartMatch("podcasts"))
    advanceUntilIdle()
    assertEquals(listOf("books"), repository.matchRequests)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "match",
              action = "library-match-all",
              libraryId = "books",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationEvent.StartScan("books"))
    viewModel.onEvent(LibraryAdministrationEvent.StartMatch("books"))
    advanceUntilIdle()
    assertEquals(emptyList<String>(), repository.scanRequests)
    assertEquals(listOf("books"), repository.matchRequests)
    collection.cancel()
  }

  @Test
  fun scanAndSynchronizationFailuresUseGenericLocalizedErrorKeys() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      TaskRepository(
        libraries = libraries("books"),
        initialTaskState =
          ServerTaskRepositoryState(
            connectionState = ServerTaskConnectionState.CONNECTED,
            snapshotKnown = true,
          ),
      )
    repository.scanResult = Result.failure(IllegalStateException("database stack trace"))
    repository.matchResult = Result.failure(IllegalStateException("database stack trace"))
    repository.retryResult = Result.failure(IllegalStateException("database stack trace"))
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationEvent.StartScan("books"))
    advanceUntilIdle()
    assertEquals(LibraryAdministrationError.GenericScanStart, viewModel.uiState.value.scanError)

    viewModel.onEvent(LibraryAdministrationEvent.StartMatch("books"))
    advanceUntilIdle()
    assertEquals(LibraryAdministrationError.GenericMatchStart, viewModel.uiState.value.matchError)

    viewModel.onEvent(LibraryAdministrationEvent.RetryTaskSynchronization("scan"))
    advanceUntilIdle()
    assertEquals(
      LibraryAdministrationError.GenericSynchronization,
      viewModel.uiState.value.taskSyncError,
    )
    collection.cancel()
  }

  @Test
  fun terminalNotificationSurvivesUiCollectorRecreationUntilAcknowledged() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      TaskRepository(
        libraries = libraries("books"),
        initialTaskState = ServerTaskRepositoryState(),
      )
    repository.taskNotification.value =
      ServerTaskNotification("scan", ServerTaskStatus.COMPLETED)

    val first = LibraryAdministrationViewModel(repository)
    val firstCollection = collectState(first)
    advanceUntilIdle()
    assertNotNull(first.uiState.value.taskNotification)
    firstCollection.cancel()

    val second = LibraryAdministrationViewModel(repository)
    val secondCollection = collectState(second)
    advanceUntilIdle()
    assertEquals(
      ServerTaskNotification("scan", ServerTaskStatus.COMPLETED),
      second.uiState.value.taskNotification,
    )

    second.consumeTaskNotification()
    assertEquals(1, repository.acknowledgements)
    assertNull(repository.taskNotification.value)
    secondCollection.cancel()
  }

  @Test
  fun deleteRequiresConfirmationAndCancellationDoesNotCallServer() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeRepository(listOf(Result.success(libraries("books"))))
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "books")

    viewModel.onEvent(LibraryAdministrationEvent.RequestDeleteLibrary("books"))
    assertEquals("books", viewModel.uiState.value.deleteConfirmationLibraryId)
    assertTrue(repository.deleteRequests.isEmpty())

    viewModel.onEvent(LibraryAdministrationEvent.CancelDeleteLibrary)
    assertNull(viewModel.uiState.value.deleteConfirmationLibraryId)
    assertTrue(repository.deleteRequests.isEmpty())
    collection.cancel()
  }

  @Test
  fun deleteIsDisabledForUnknownActiveOrDisconnectedTaskState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeRepository(listOf(Result.success(libraries("books"))))
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(!viewModel.uiState.value.canDelete("books"))
    enableReorder(viewModel, "books")
    viewModel.onEvent(
      LibraryAdministrationEvent.SetTaskState("books", LibraryAdministrationTaskState.ACTIVE)
    )
    viewModel.onEvent(LibraryAdministrationEvent.RequestDeleteLibrary("books"))
    assertNull(viewModel.uiState.value.deleteConfirmationLibraryId)

    viewModel.onEvent(
      LibraryAdministrationEvent.SetTaskState("books", LibraryAdministrationTaskState.IDLE)
    )
    viewModel.onEvent(
      LibraryAdministrationEvent.SetConnectionState(
        LibraryAdministrationConnectionState.DISCONNECTED
      )
    )
    viewModel.onEvent(LibraryAdministrationEvent.RequestDeleteLibrary("books"))
    assertNull(viewModel.uiState.value.deleteConfirmationLibraryId)
    collection.cancel()
  }

  @Test
  fun successfulDeleteRemovesLibraryAndSelectsNextLibrary() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("first", "books", "third")
    val repository =
      FakeRepository(
        results = listOf(Result.success(initial)),
        deleteResults = listOf(Result.success(Unit)),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "first", "books", "third")

    viewModel.onEvent(LibraryAdministrationEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdministrationEvent.ConfirmDeleteLibrary)
    advanceUntilIdle()

    assertEquals(listOf("first", "third"), viewModel.uiState.value.libraries.map { it.id })
    assertNull(viewModel.uiState.value.deleteError)
    assertEquals(listOf("books"), repository.deleteRequests)
    collection.cancel()
  }

  @Test
  fun deletingFinalLibraryLeavesEmptyCatalogAndNoActiveLibrary() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        results = listOf(Result.success(libraries("books"))),
        deleteResults = listOf(Result.success(Unit)),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "books")

    viewModel.onEvent(LibraryAdministrationEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdministrationEvent.ConfirmDeleteLibrary)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.libraries.isEmpty())
    collection.cancel()
  }

  @Test
  fun failedDeletePreservesLibraryAndOffersRetryWithSafeError() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        results = listOf(Result.success(libraries("books"))),
        deleteResults = listOf(Result.failure(IllegalStateException("database details"))),
      )
    val viewModel = LibraryAdministrationViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(viewModel, "books")

    viewModel.onEvent(LibraryAdministrationEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdministrationEvent.ConfirmDeleteLibrary)
    advanceUntilIdle()

    assertEquals(listOf("books"), viewModel.uiState.value.libraries.map { it.id })
    assertEquals(LibraryAdministrationError.GenericDelete, viewModel.uiState.value.deleteError)
    assertEquals("books", viewModel.uiState.value.deleteRetryLibraryId)
    viewModel.onEvent(LibraryAdministrationEvent.RetryDeleteLibrary)
    assertEquals("books", viewModel.uiState.value.deleteConfirmationLibraryId)
    collection.cancel()
  }

  private fun TestScope.collectState(viewModel: LibraryAdministrationViewModel): Job =
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private fun enableReorder(viewModel: LibraryAdministrationViewModel, vararg ids: String) {
    viewModel.onEvent(
      LibraryAdministrationEvent.SetConnectionState(LibraryAdministrationConnectionState.CONNECTED)
    )
    ids.forEach { id ->
      viewModel.onEvent(
        LibraryAdministrationEvent.SetTaskState(id, LibraryAdministrationTaskState.IDLE)
      )
    }
  }

  private fun libraries(vararg ids: String): List<LibraryAdministrationLibrary> =
    ids.mapIndexed { index, id ->
      LibraryAdministrationLibrary(
        id = id,
        name = id.replaceFirstChar(Char::uppercaseChar),
        mediaType = LibraryAdministrationMediaType.BOOK,
        displayOrder = index + 1,
      )
    }

  private class FakeRepository(
    results: List<Result<List<LibraryAdministrationLibrary>>>,
    reorderResults: List<Result<List<LibraryAdministrationLibrary>>> = emptyList(),
    deleteResults: List<Result<Unit>> = emptyList(),
  ) :
    LibraryAdministrationContract {
    private val pendingResults = ArrayDeque(results)
    private val pendingReorderResults = ArrayDeque(reorderResults)
    private val pendingDeleteResults = ArrayDeque(deleteResults)
    var loadCalls = 0
      private set
    val reorderRequests =
      mutableListOf<
        Pair<List<LibraryAdministrationLibrary>, List<LibraryAdministrationLibrary>>
      >()
    val deleteRequests = mutableListOf<String>()

    override suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>> {
      loadCalls += 1
      return pendingResults.removeFirst()
    }

    override suspend fun reorderLibraries(
      libraries: List<LibraryAdministrationLibrary>
    ): Result<List<LibraryAdministrationLibrary>> {
      val result = pendingReorderResults.removeFirst()
      reorderRequests += libraries to result.getOrNull().orEmpty()
      return result
    }

    override suspend fun deleteLibrary(libraryId: String): Result<Unit> {
      deleteRequests += libraryId
      return pendingDeleteResults.removeFirst()
    }
  }

  private class ControlledRepository(
    private val initial: List<LibraryAdministrationLibrary>,
    private val responses: ArrayDeque<CompletableDeferred<Result<List<LibraryAdministrationLibrary>>>>,
  ) : LibraryAdministrationContract {
    override suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>> =
      Result.success(initial)

    override suspend fun reorderLibraries(
      libraries: List<LibraryAdministrationLibrary>
    ): Result<List<LibraryAdministrationLibrary>> = responses.removeFirst().await()
  }

  private class TaskRepository(
    private val libraries: List<LibraryAdministrationLibrary>,
    initialTaskState: ServerTaskRepositoryState,
  ) : LibraryAdministrationContract {
    val mutableTaskState = MutableStateFlow(initialTaskState)
    val taskNotification = MutableStateFlow<ServerTaskNotification?>(null)
    var scanResult: Result<Unit> = Result.success(Unit)
    var matchResult: Result<Unit> = Result.success(Unit)
    var retryResult: Result<Unit> = Result.success(Unit)
    val scanRequests = mutableListOf<String>()
    val matchRequests = mutableListOf<String>()
    var acknowledgements = 0

    override val taskState: StateFlow<ServerTaskRepositoryState>
      get() = mutableTaskState

    override val taskNotifications: StateFlow<ServerTaskNotification?>
      get() = taskNotification

    override suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>> =
      Result.success(libraries)

    override suspend fun refreshTasks(): Result<Unit> = Result.success(Unit)

    override suspend fun startScan(libraryId: String): Result<Unit> {
      scanRequests += libraryId
      return scanResult
    }

    override suspend fun startMatch(libraryId: String): Result<Unit> {
      matchRequests += libraryId
      return matchResult
    }

    override suspend fun retryTaskSynchronization(taskId: String): Result<Unit> = retryResult

    override fun acknowledgeTaskNotification(taskId: String) {
      acknowledgements += 1
      taskNotification.value = null
    }

    override suspend fun reorderLibraries(
      libraries: List<LibraryAdministrationLibrary>
    ): Result<List<LibraryAdministrationLibrary>> = Result.success(libraries)
  }
}
