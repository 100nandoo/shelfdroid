package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import java.util.ArrayDeque
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

  private fun TestScope.collectState(viewModel: LibraryAdministrationViewModel): Job =
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private class FakeRepository(results: List<Result<List<LibraryAdministrationLibrary>>>) :
    LibraryAdministrationContract {
    private val pendingResults = ArrayDeque(results)
    var loadCalls = 0
      private set

    override suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>> {
      loadCalls += 1
      return pendingResults.removeFirst()
    }
  }
}
