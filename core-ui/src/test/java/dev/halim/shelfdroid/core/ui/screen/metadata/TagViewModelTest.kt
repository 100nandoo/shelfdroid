package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.GenreMutation
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilsContract
import dev.halim.shelfdroid.core.data.metadata.TagApiState
import dev.halim.shelfdroid.core.data.metadata.TagMutation
import dev.halim.shelfdroid.core.ui.screen.metadata.tag.TagEvent
import dev.halim.shelfdroid.core.ui.screen.metadata.tag.TagViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagViewModelTest {

  @Test
  fun loadSuccess_sortsTagsAndExposesReadyContent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      TagViewModel(FakeRepository(loadResults = listOf(Result.success(listOf("zeta", "Alpha")))))
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(GenericState.Success, viewModel.uiState.value.state)
    assertEquals(listOf("Alpha", "zeta"), viewModel.uiState.value.tags)
    assertEquals(TagApiState.Idle, viewModel.uiState.value.apiState)
    collection.cancel()
  }

  @Test
  fun loadSuccess_withNoTagsExposesEmptyContent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = TagViewModel(FakeRepository(loadResults = listOf(Result.success(emptyList()))))
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
      TagViewModel(
        FakeRepository(loadResults = listOf(Result.failure(IllegalStateException("offline"))))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.state is GenericState.Failure)
    assertEquals(TagApiState.Failure("offline"), viewModel.uiState.value.apiState)
    assertFalse((viewModel.uiState.value.apiState as TagApiState.Failure).accessDenied)
    collection.cancel()
  }

  @Test
  fun accessDenied_exposesNonRetryableFailure() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      TagViewModel(
        FakeRepository(loadResults = listOf(Result.failure(MetadataAccessDeniedException())))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertEquals(
      TagApiState.Failure(
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
    val viewModel = TagViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(TagEvent.BeginRename("canonical"))
    viewModel.onEvent(TagEvent.UpdateRenameDraft("changed"))
    viewModel.onEvent(TagEvent.ConfirmRename)
    advanceUntilIdle()

    assertEquals(listOf("canonical"), viewModel.uiState.value.tags)
    assertEquals(TagApiState.Failure("rejected"), viewModel.uiState.value.apiState)
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
    val viewModel = TagViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(TagEvent.BeginRename("old"))
    viewModel.onEvent(TagEvent.UpdateRenameDraft("new"))
    viewModel.onEvent(TagEvent.ConfirmRename)
    advanceUntilIdle()

    assertEquals(listOf("new"), viewModel.uiState.value.tags)
    assertEquals(
      TagApiState.RenameSuccess(2, merged = true),
      viewModel.uiState.value.apiState,
    )
    assertEquals(2, repository.loadCalls)
    collection.cancel()
  }

  private fun TestScope.collectState(viewModel: TagViewModel): Job =
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private class FakeRepository(
    loadResults: List<Result<List<String>>>,
    private val renameResult: Result<TagMutation> = Result.success(TagMutation(0)),
  ) : MetadataUtilsContract {
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

    override suspend fun loadGenres(): Result<List<String>> = Result.success(emptyList())

    override suspend fun renameGenre(genre: String, newGenre: String): Result<GenreMutation> =
      Result.success(GenreMutation(updatedItemCount = 0))

    override suspend fun deleteGenre(genre: String): Result<GenreMutation> =
      Result.success(GenreMutation(updatedItemCount = 0))
  }
}
