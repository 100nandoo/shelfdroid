package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminContract
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMutationResult
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canDelete
import dev.halim.shelfdroid.core.data.task.ServerTask
import dev.halim.shelfdroid.core.data.task.ServerTaskConnectionState
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import dev.halim.shelfdroid.core.data.task.ServerTaskAction
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
class LibraryAdminViewModelTest {

  @Test
  fun loadSuccess_preservesServerOrderAndIdentity() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val libraries =
      listOf(
        LibraryAdminLibrary(
          "podcasts",
          "Podcasts",
          LibraryAdminMediaType.PODCAST,
          4,
        ),
        LibraryAdminLibrary(
          "books",
          "Books",
          LibraryAdminMediaType.BOOK,
          9,
        ),
      )
    val viewModel = LibraryAdminViewModel(FakeRepository(listOf(Result.success(libraries))))
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
              LibraryAdminLibrary(
                "books",
                "Books",
                LibraryAdminMediaType.BOOK,
                0,
              )
            )
          ),
        ),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.state is GenericState.Failure)
    assertNull((viewModel.uiState.value.state as GenericState.Failure).errorMessage)
    assertTrue(viewModel.uiState.value.libraries.isEmpty())

    viewModel.onEvent(LibraryAdminEvent.Refresh)
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
        LibraryAdminLibrary(
          id = "new-books",
          name = "New Books",
          mediaType = LibraryAdminMediaType.BOOK,
          displayOrder = 1,
        ),
        LibraryAdminLibrary(
          id = "books",
          name = "Renamed Books",
          mediaType = LibraryAdminMediaType.BOOK,
          displayOrder = 2,
        ),
      )
    val repository =
      FakeRepository(
        listOf(Result.success(initial), Result.success(refreshed)),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    // The server snapshot includes an added, updated, removed, and reordered result even though
    // no corresponding socket event was delivered to this screen.
    viewModel.onEvent(LibraryAdminEvent.Refresh)
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
              LibraryAdminLibrary(
                "books",
                "Books",
                LibraryAdminMediaType.BOOK,
                0,
              )
            )
          ),
          Result.failure(IllegalStateException("offline")),
        ),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminEvent.Refresh)
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
    val repository =
      FakeRepository(
        listOf(Result.success(initial)),
        listOf(
          Result.success(LibraryAdminMutationResult.Accepted(accepted))
        ),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books", "podcasts")

    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
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
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books", "podcasts")

    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    advanceUntilIdle()

    assertEquals(initial, viewModel.uiState.value.libraries)
    assertEquals("offline", viewModel.uiState.value.reorderError)
    collection.cancel()
  }

  @Test
  fun partialMove_keepsAcceptedOrderAndRetriesSynchronizationWithoutRepeatingReorder() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts")
    val accepted = libraries("podcasts", "books")
    val repository =
      FakeRepository(
        results = listOf(Result.success(initial)),
        reorderResults =
          listOf(
            Result.success(
              LibraryAdminMutationResult.AcceptedButNotSynchronized(
                value = accepted,
                error = IllegalStateException("catalog refresh failed"),
              )
            )
          ),
        synchronizationResults =
          listOf(
            Result.failure(IllegalStateException("offline")),
            Result.success(Unit),
          ),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books", "podcasts")

    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    advanceUntilIdle()

    assertEquals(accepted, viewModel.uiState.value.libraries)
    assertEquals(
      LibraryAdminError.GenericReorderSynchronization,
      viewModel.uiState.value.reorderSyncError,
    )
    assertEquals(accepted, viewModel.uiState.value.reorderRetryOrder)
    assertEquals(1, repository.reorderRequests.size)

    viewModel.onEvent(LibraryAdminEvent.RetryReorderSynchronization)
    advanceUntilIdle()

    assertEquals(accepted, viewModel.uiState.value.libraries)
    assertEquals(
      LibraryAdminError.GenericReorderSynchronization,
      viewModel.uiState.value.reorderSyncError,
    )
    assertEquals(1, repository.synchronizeCalls)
    assertEquals(1, repository.reorderRequests.size)

    viewModel.onEvent(LibraryAdminEvent.RetryReorderSynchronization)
    advanceUntilIdle()

    assertEquals(null, viewModel.uiState.value.reorderSyncError)
    assertEquals(null, viewModel.uiState.value.reorderRetryOrder)
    assertEquals(2, repository.synchronizeCalls)
    assertEquals(1, repository.reorderRequests.size)
    collection.cancel()
  }

  @Test
  fun moveLibrary_isDisabledForUnknownActiveOrDisconnectedTaskState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts")
    val repository = FakeRepository(listOf(Result.success(initial)))
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        connectionState = ServerTaskConnectionState.CONNECTED,
        snapshotKnown = false,
      )
    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "scan",
              action = ServerTaskAction.LibraryScan,
              libraryId = "books",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "scan",
              action = ServerTaskAction.LibraryScan,
              libraryId = "podcasts",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        connectionState = ServerTaskConnectionState.DISCONNECTED,
        tasks = emptyList(),
      )
    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    assertEquals(0, repository.reorderRequests.size)
    collection.cancel()
  }

  @Test
  fun newestCompletedReorderAcknowledgementWinsOverOlderIntent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val initial = libraries("books", "podcasts", "third")
    val firstAccepted = libraries("podcasts", "books", "third")
    val secondAccepted = libraries("podcasts", "third", "books")
    val first =
      CompletableDeferred<
        Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>>
      >()
    val second =
      CompletableDeferred<
        Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>>
      >()
    val repository =
      ControlledRepository(
        initial = initial,
        responses = ArrayDeque(listOf(first, second)),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books", "podcasts", "third")

    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    viewModel.onEvent(LibraryAdminEvent.MoveLibrary("books", 1))
    second.complete(
      Result.success(LibraryAdminMutationResult.Accepted(secondAccepted))
    )
    advanceUntilIdle()
    first.complete(
      Result.success(LibraryAdminMutationResult.Accepted(firstAccepted))
    )
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
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminEvent.StartScan("books"))
    advanceUntilIdle()
    assertEquals(listOf("books"), repository.scanRequests)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "scan",
              action = ServerTaskAction.LibraryScan,
              libraryId = "books",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminEvent.StartScan("books"))
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
            LibraryAdminLibrary(
              "podcasts",
              "Podcasts",
              LibraryAdminMediaType.PODCAST,
              2,
            ),
          ),
        initialTaskState =
          ServerTaskRepositoryState(
            connectionState = ServerTaskConnectionState.CONNECTED,
            snapshotKnown = true,
          ),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminEvent.StartMatch("books"))
    viewModel.onEvent(LibraryAdminEvent.StartMatch("podcasts"))
    advanceUntilIdle()
    assertEquals(listOf("books"), repository.matchRequests)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "match",
              action = ServerTaskAction.BookMatching,
              libraryId = "books",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminEvent.StartScan("books"))
    viewModel.onEvent(LibraryAdminEvent.StartMatch("books"))
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
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminEvent.StartScan("books"))
    advanceUntilIdle()
    assertEquals(LibraryAdminError.GenericScanStart, viewModel.uiState.value.scanError)

    viewModel.onEvent(LibraryAdminEvent.StartMatch("books"))
    advanceUntilIdle()
    assertEquals(LibraryAdminError.GenericMatchStart, viewModel.uiState.value.matchError)

    viewModel.onEvent(LibraryAdminEvent.RetryTaskSynchronization("scan"))
    advanceUntilIdle()
    assertEquals(
      LibraryAdminError.GenericSynchronization,
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
      ServerTaskNotification("scan", ServerTaskStatus.COMPLETED, ServerTaskAction.LibraryScan)

    val first = LibraryAdminViewModel(repository)
    val firstCollection = collectState(first)
    advanceUntilIdle()
    assertNotNull(first.uiState.value.taskNotification)
    firstCollection.cancel()

    val second = LibraryAdminViewModel(repository)
    val secondCollection = collectState(second)
    advanceUntilIdle()
    assertEquals(
      ServerTaskNotification("scan", ServerTaskStatus.COMPLETED, ServerTaskAction.LibraryScan),
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
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books")

    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
    assertEquals("books", viewModel.uiState.value.deleteConfirmationLibraryId)
    assertTrue(repository.deleteRequests.isEmpty())

    viewModel.onEvent(LibraryAdminEvent.CancelDeleteLibrary)
    assertNull(viewModel.uiState.value.deleteConfirmationLibraryId)
    assertTrue(repository.deleteRequests.isEmpty())
    collection.cancel()
  }

  @Test
  fun deleteIsDisabledForUnknownActiveOrDisconnectedTaskState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeRepository(listOf(Result.success(libraries("books"))))
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(!viewModel.uiState.value.canDelete("books"))
    enableReorder(repository, "books")
    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        tasks =
          listOf(
            ServerTask(
              id = "scan",
              action = ServerTaskAction.LibraryScan,
              libraryId = "books",
              status = ServerTaskStatus.ACTIVE,
            )
          )
      )
    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
    assertNull(viewModel.uiState.value.deleteConfirmationLibraryId)

    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        connectionState = ServerTaskConnectionState.DISCONNECTED,
        tasks = emptyList(),
      )
    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
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
        deleteResults =
          listOf(Result.success(LibraryAdminMutationResult.Accepted(Unit))),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "first", "books", "third")

    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdminEvent.ConfirmDeleteLibrary)
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
        deleteResults =
          listOf(Result.success(LibraryAdminMutationResult.Accepted(Unit))),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books")

    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdminEvent.ConfirmDeleteLibrary)
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
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "books")

    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdminEvent.ConfirmDeleteLibrary)
    advanceUntilIdle()

    assertEquals(listOf("books"), viewModel.uiState.value.libraries.map { it.id })
    assertEquals(LibraryAdminError.GenericDelete, viewModel.uiState.value.deleteError)
    assertEquals("books", viewModel.uiState.value.deleteRetryLibraryId)
    viewModel.onEvent(LibraryAdminEvent.RetryDeleteLibrary)
    assertEquals("books", viewModel.uiState.value.deleteConfirmationLibraryId)
    collection.cancel()
  }

  @Test
  fun partialDeleteRemovesLibraryAndRetriesSynchronizationWithoutRepeatingDelete() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        results = listOf(Result.success(libraries("first", "books", "third"))),
        deleteResults =
          listOf(
            Result.success(
              LibraryAdminMutationResult.AcceptedButNotSynchronized(
                value = Unit,
                error = IllegalStateException("catalog refresh failed"),
              )
            )
          ),
        synchronizationResults =
          listOf(
            Result.failure(IllegalStateException("offline")),
            Result.success(Unit),
          ),
      )
    val viewModel = LibraryAdminViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    enableReorder(repository, "first", "books", "third")

    viewModel.onEvent(LibraryAdminEvent.RequestDeleteLibrary("books"))
    viewModel.onEvent(LibraryAdminEvent.ConfirmDeleteLibrary)
    advanceUntilIdle()

    assertEquals(
      listOf("first", "third"),
      viewModel.uiState.value.libraries.map { it.id },
    )
    assertEquals(
      LibraryAdminError.GenericDeleteSynchronization,
      viewModel.uiState.value.deleteSyncError,
    )
    assertEquals(listOf("books"), repository.deleteRequests)

    viewModel.onEvent(LibraryAdminEvent.RetryDeleteSynchronization)
    advanceUntilIdle()

    assertEquals(
      LibraryAdminError.GenericDeleteSynchronization,
      viewModel.uiState.value.deleteSyncError,
    )
    assertEquals(1, repository.synchronizeCalls)
    assertEquals(listOf("books"), repository.deleteRequests)

    viewModel.onEvent(LibraryAdminEvent.RetryDeleteSynchronization)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.deleteSyncError)
    assertNull(viewModel.uiState.value.deleteRetryLibraryId)
    assertEquals(2, repository.synchronizeCalls)
    assertEquals(listOf("books"), repository.deleteRequests)
    collection.cancel()
  }

  private fun TestScope.collectState(viewModel: LibraryAdminViewModel): Job =
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private fun enableReorder(repository: TaskStateDriver, vararg ids: String) {
    repository.mutableTaskState.value =
      repository.mutableTaskState.value.copy(
        connectionState = ServerTaskConnectionState.CONNECTED,
        snapshotKnown = true,
      )
  }

  private fun libraries(vararg ids: String): List<LibraryAdminLibrary> =
    ids.mapIndexed { index, id ->
      LibraryAdminLibrary(
        id = id,
        name = id.replaceFirstChar(Char::uppercaseChar),
        mediaType = LibraryAdminMediaType.BOOK,
        displayOrder = index + 1,
      )
    }

  private interface TaskStateDriver {
    val mutableTaskState: MutableStateFlow<ServerTaskRepositoryState>
  }

  private class FakeRepository(
    results: List<Result<List<LibraryAdminLibrary>>>,
    reorderResults: List<Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>>> =
      emptyList(),
    deleteResults: List<Result<LibraryAdminMutationResult<Unit>>> = emptyList(),
    synchronizationResults: List<Result<Unit>> = emptyList(),
  ) :
    LibraryAdminContract, TaskStateDriver {
    override val mutableTaskState = MutableStateFlow(ServerTaskRepositoryState())
    private val pendingResults = ArrayDeque(results)
    private val pendingReorderResults = ArrayDeque(reorderResults)
    private val pendingDeleteResults = ArrayDeque(deleteResults)
    private val pendingSynchronizationResults = ArrayDeque(synchronizationResults)
    var loadCalls = 0
      private set
    var synchronizeCalls = 0
      private set
    val reorderRequests =
      mutableListOf<
        Pair<List<LibraryAdminLibrary>, List<LibraryAdminLibrary>>
      >()
    val deleteRequests = mutableListOf<String>()

    override val taskState: StateFlow<ServerTaskRepositoryState>
      get() = mutableTaskState

    override suspend fun loadLibraries(): Result<List<LibraryAdminLibrary>> {
      loadCalls += 1
      return pendingResults.removeFirst()
    }

    override suspend fun reorderLibraries(
      libraries: List<LibraryAdminLibrary>
    ): Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>> {
      val result = pendingReorderResults.removeFirst()
      reorderRequests += libraries to result.getOrNull()?.let { outcome ->
        when (outcome) {
          is LibraryAdminMutationResult.Accepted -> outcome.value
          is LibraryAdminMutationResult.AcceptedButNotSynchronized -> outcome.value
        }
      }.orEmpty()
      return result
    }

    override suspend fun synchronizeLibraries(): Result<Unit> {
      synchronizeCalls += 1
      return if (pendingSynchronizationResults.isEmpty()) {
        Result.success(Unit)
      } else {
        pendingSynchronizationResults.removeFirst()
      }
    }

    override suspend fun deleteLibrary(
      libraryId: String
    ): Result<LibraryAdminMutationResult<Unit>> {
      deleteRequests += libraryId
      return pendingDeleteResults.removeFirst()
    }
  }

  private class ControlledRepository(
    private val initial: List<LibraryAdminLibrary>,
    private val responses:
      ArrayDeque<
        CompletableDeferred<
          Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>>
        >
      >,
  ) : LibraryAdminContract, TaskStateDriver {
    override val mutableTaskState = MutableStateFlow(ServerTaskRepositoryState())

    override val taskState: StateFlow<ServerTaskRepositoryState>
      get() = mutableTaskState

    override suspend fun loadLibraries(): Result<List<LibraryAdminLibrary>> =
      Result.success(initial)

    override suspend fun reorderLibraries(
      libraries: List<LibraryAdminLibrary>
    ): Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>> =
      responses.removeFirst().await()
  }

  private class TaskRepository(
    private val libraries: List<LibraryAdminLibrary>,
    initialTaskState: ServerTaskRepositoryState,
  ) : LibraryAdminContract, TaskStateDriver {
    override val mutableTaskState = MutableStateFlow(initialTaskState)
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

    override suspend fun loadLibraries(): Result<List<LibraryAdminLibrary>> =
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
      libraries: List<LibraryAdminLibrary>
    ): Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>> =
      Result.success(LibraryAdminMutationResult.Accepted(libraries))
  }
}
