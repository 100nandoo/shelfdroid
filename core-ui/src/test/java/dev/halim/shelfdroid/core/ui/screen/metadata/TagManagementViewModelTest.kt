package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract
import dev.halim.shelfdroid.core.data.metadata.TagManagementApiState
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagManagementViewModelTest {

  @Test
  fun loadSuccess_sortsTagsAndExposesReadyContent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      TagManagementViewModel(
        FakeRepository(loadResults = listOf(Result.success(listOf("zeta", "Alpha"))))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(listOf("Alpha", "zeta"), viewModel.uiState.value.tags)
    assertEquals(TagManagementApiState.Idle, viewModel.uiState.value.apiState)
    collection.cancel()
  }

  @Test
  fun loadSuccess_withNoTagsExposesEmptyContent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      TagManagementViewModel(FakeRepository(loadResults = listOf(Result.success(emptyList()))))
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertTrue(viewModel.uiState.value.tags.isEmpty())
    collection.cancel()
  }

  @Test
  fun loadFailure_exposesRetryableFailure() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      TagManagementViewModel(
        FakeRepository(loadResults = listOf(Result.failure(IllegalStateException("offline"))))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.state is GenericState.Failure)
    assertEquals(TagManagementApiState.Failure("offline"), viewModel.uiState.value.apiState)
    assertFalse((viewModel.uiState.value.apiState as TagManagementApiState.Failure).accessDenied)
    collection.cancel()
  }

  @Test
  fun accessDenied_exposesNonRetryableFailure() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      TagManagementViewModel(
        FakeRepository(loadResults = listOf(Result.failure(MetadataAccessDeniedException())))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(
      TagManagementApiState.Failure(
        "The Audiobookshelf server denied access to this administrative operation.",
        accessDenied = true,
      ),
      viewModel.uiState.value.apiState,
    )
    collection.cancel()
  }

  @Test
  fun failedRename_preservesCanonicalVisibleTags() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        loadResults = listOf(Result.success(listOf("canonical"))),
        renameResult = Result.failure(IllegalStateException("rejected")),
      )
    val viewModel = TagManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(TagManagementEvent.BeginRename("canonical"))
    viewModel.onEvent(TagManagementEvent.UpdateRenameDraft("changed"))
    viewModel.onEvent(TagManagementEvent.ConfirmRename)
    advanceUntilIdle()

    assertEquals(listOf("canonical"), viewModel.uiState.value.tags)
    assertEquals(TagManagementApiState.Failure("rejected"), viewModel.uiState.value.apiState)
    assertEquals(1, repository.renameCalls)
    assertEquals(1, repository.loadCalls)
    collection.cancel()
  }

  @Test
  fun successfulRename_refreshesVisibleTagsAfterMutation() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        loadResults =
          listOf(
            Result.success(listOf("old")),
            Result.success(listOf("new")),
          ),
        renameResult = Result.success(TagMutation(updatedItemCount = 2, merged = true)),
      )
    val viewModel = TagManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(TagManagementEvent.BeginRename("old"))
    viewModel.onEvent(TagManagementEvent.UpdateRenameDraft("new"))
    viewModel.onEvent(TagManagementEvent.ConfirmRename)
    advanceUntilIdle()

    assertEquals(listOf("new"), viewModel.uiState.value.tags)
    assertEquals(
      TagManagementApiState.RenameSuccess(2, merged = true),
      viewModel.uiState.value.apiState,
    )
    assertEquals(2, repository.loadCalls)
    collection.cancel()
  }

  private fun TestScope.collectState(viewModel: TagManagementViewModel): Job =
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private class FakeRepository(
    loadResults: List<Result<List<String>>>,
    private val renameResult: Result<TagMutation> = Result.success(TagMutation(0)),
  ) : MetadataUtilitiesRepositoryContract {
    private val pendingLoads = ArrayDeque(loadResults)
    var loadCalls = 0
      private set
    var renameCalls = 0
      private set

    override suspend fun loadTags(): Result<List<String>> {
      loadCalls += 1
      return pendingLoads.removeFirst()
    }

    override suspend fun renameTag(tag: String, newTag: String): Result<TagMutation> {
      renameCalls += 1
      return renameResult
    }

    override suspend fun deleteTag(tag: String): Result<TagMutation> =
      Result.success(TagMutation(updatedItemCount = 0))

  }
}
