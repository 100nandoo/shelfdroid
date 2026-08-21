package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.GenreManagementApiState
import dev.halim.shelfdroid.core.data.metadata.GenreMutation
import dev.halim.shelfdroid.core.data.metadata.GenreOperation
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract
import dev.halim.shelfdroid.core.data.metadata.TagMutation
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenreManagementViewModelTest {

  @Test
  fun loadSuccess_sortsGenresAndExposesReadyContent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      GenreManagementViewModel(
        FakeRepository(loadResults = listOf(Result.success(listOf("zeta", "Alpha"))))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(listOf("Alpha", "zeta"), viewModel.uiState.value.genres)
    collection.cancel()
  }

  @Test
  fun accessDenied_exposesNonRetryableFailure() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      GenreManagementViewModel(
        FakeRepository(
          loadResults = listOf(Result.failure(MetadataAccessDeniedException()))
        )
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    val state = viewModel.uiState.value.apiState as GenreManagementApiState.Failure
    assertTrue(state.accessDenied)
    collection.cancel()
  }

  @Test
  fun successfulRename_reportsCountAndReloadsGenres() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        loadResults =
          listOf(
            Result.success(listOf("old")),
            Result.success(listOf("new")),
          ),
        renameResult = Result.success(GenreMutation(updatedItemCount = 2, merged = true)),
      )
    val viewModel = GenreManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(GenreManagementEvent.BeginRename("old"))
    viewModel.onEvent(GenreManagementEvent.UpdateRenameDraft("new"))
    viewModel.onEvent(GenreManagementEvent.ConfirmRename)
    advanceUntilIdle()

    assertEquals(listOf("new"), viewModel.uiState.value.genres)
    assertEquals(GenreManagementApiState.RenameSuccess(2, merged = true), viewModel.uiState.value.apiState)
    assertEquals(2, repository.loadCalls)
    collection.cancel()
  }

  @Test
  fun failedDelete_preservesCurrentGenres() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        loadResults = listOf(Result.success(listOf("current"))),
        deleteResult = Result.failure(IllegalStateException("offline")),
      )
    val viewModel = GenreManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(GenreManagementEvent.BeginDelete("current"))
    viewModel.onEvent(GenreManagementEvent.ConfirmDelete)
    advanceUntilIdle()

    assertEquals(listOf("current"), viewModel.uiState.value.genres)
    assertEquals(
      GenreManagementApiState.Failure("offline", operation = GenreOperation.Delete),
      viewModel.uiState.value.apiState,
    )
    collection.cancel()
  }

  @Test
  fun failedRename_reportsRenameOperationAndPreservesCurrentGenres() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        loadResults = listOf(Result.success(listOf("current"))),
        renameResult = Result.failure(IllegalStateException("offline")),
      )
    val viewModel = GenreManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(GenreManagementEvent.BeginRename("current"))
    viewModel.onEvent(GenreManagementEvent.UpdateRenameDraft("changed"))
    viewModel.onEvent(GenreManagementEvent.ConfirmRename)
    advanceUntilIdle()

    assertEquals(listOf("current"), viewModel.uiState.value.genres)
    assertEquals(
      GenreManagementApiState.Failure("offline", operation = GenreOperation.Rename),
      viewModel.uiState.value.apiState,
    )
    collection.cancel()
  }

  private fun TestScope.collectState(viewModel: GenreManagementViewModel): Job =
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private class FakeRepository(
    loadResults: List<Result<List<String>>>,
    private val renameResult: Result<GenreMutation> = Result.success(GenreMutation(0)),
    private val deleteResult: Result<GenreMutation> = Result.success(GenreMutation(0)),
  ) : MetadataUtilitiesRepositoryContract {
    private val pendingLoads = ArrayDeque(loadResults)
    var loadCalls = 0
      private set

    override suspend fun loadTags(): Result<List<String>> = Result.success(emptyList())

    override suspend fun renameTag(tag: String, newTag: String): Result<TagMutation> =
      Result.success(TagMutation(0))

    override suspend fun deleteTag(tag: String): Result<TagMutation> = Result.success(TagMutation(0))

    override suspend fun loadGenres(): Result<List<String>> {
      loadCalls += 1
      return pendingLoads.removeFirst()
    }

    override suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> =
      renameResult

    override suspend fun deleteGenre(genre: String): Result<GenreMutation> = deleteResult
  }
}
