package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationConnectionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationTaskState
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
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
    assertTrue(viewModel.uiState.value.libraries.isEmpty())

    viewModel.onEvent(LibraryAdministrationEvent.Refresh)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(listOf("Books"), viewModel.uiState.value.libraries.map { it.name })
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

    viewModel.onEvent(LibraryAdministrationEvent.SetConnectionState(LibraryAdministrationConnectionState.CONNECTED))
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
  ) :
    LibraryAdministrationContract {
    private val pendingResults = ArrayDeque(results)
    private val pendingReorderResults = ArrayDeque(reorderResults)
    var loadCalls = 0
      private set
    val reorderRequests = mutableListOf<Pair<List<LibraryAdministrationLibrary>, List<LibraryAdministrationLibrary>>>()

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
}
