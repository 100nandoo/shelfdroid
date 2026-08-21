package dev.halim.shelfdroid.core.ui.screen.metadata

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementApiState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementDialog
import dev.halim.shelfdroid.core.data.metadata.MetadataAccessDeniedException
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomMetadataProviderManagementViewModelTest {

  @Test
  fun create_successReloadsProviderAndClearsDraftSecret() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val provider = provider(authHeaderValue = "Bearer secret")
    val repository =
      FakeRepository(
        loadResults = listOf(Result.success(emptyList()), Result.success(listOf(provider))),
        createResult = Result.success(provider),
      )
    val viewModel = CustomMetadataProviderManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateName("Community"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateUrl("https://provider.example"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateAuthHeader("Bearer secret"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SetAuthHeaderVisible(true))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SubmitCreate)
    advanceUntilIdle()

    assertEquals(2, repository.loadCalls)
    assertEquals("Bearer secret", repository.createdAuthHeader)
    assertEquals(listOf(provider), viewModel.uiState.value.providers)
    assertEquals("", viewModel.uiState.value.authHeaderDraft)
    assertTrue(!viewModel.uiState.value.authHeaderVisible)
    collection.cancel()
  }

  @Test
  fun delete_requiresConfirmationThenReloadsWithoutInventedCount() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val provider = provider()
    val repository =
      FakeRepository(
        loadResults = listOf(Result.success(listOf(provider)), Result.success(emptyList()))
      )
    val viewModel = CustomMetadataProviderManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(CustomMetadataProviderManagementEvent.BeginDelete(provider))
    assertTrue(viewModel.uiState.value.dialog is CustomMetadataProviderManagementDialog.Delete)
    assertEquals(0, repository.deleteCalls)

    viewModel.onEvent(CustomMetadataProviderManagementEvent.ConfirmDelete)
    advanceUntilIdle()

    assertEquals(1, repository.deleteCalls)
    assertTrue(viewModel.uiState.value.providers.isEmpty())
    assertTrue(viewModel.uiState.value.apiState is CustomMetadataProviderManagementApiState.DeleteSuccess)
    collection.cancel()
  }

  @Test
  fun leavingScreenClearsDraftAndReconcealsProviderHeaders() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val provider = provider(authHeaderValue = "Bearer secret")
    val viewModel =
      CustomMetadataProviderManagementViewModel(
        FakeRepository(loadResults = listOf(Result.success(listOf(provider))))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateAuthHeader("transient"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SetAuthHeaderVisible(true))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SetProviderVisible(provider.id, true))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.ClearSensitiveState)

    assertEquals("", viewModel.uiState.value.authHeaderDraft)
    assertTrue(!viewModel.uiState.value.authHeaderVisible)
    assertTrue(viewModel.uiState.value.revealedProviderIds.isEmpty())
    assertEquals(null, viewModel.uiState.value.providers.single().authHeaderValue)
    collection.cancel()
  }

  @Test
  fun accessDenied_isNonRetryable() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel =
      CustomMetadataProviderManagementViewModel(
        FakeRepository(loadResults = listOf(Result.failure(MetadataAccessDeniedException())))
      )
    val collection = collectState(viewModel)
    advanceUntilIdle()

    val failure =
      viewModel.uiState.value.apiState as CustomMetadataProviderManagementApiState.Failure
    assertTrue(failure.accessDenied)
    assertTrue(viewModel.uiState.value.state is GenericState.Failure)
    collection.cancel()
  }

  @Test
  fun failedCreatePreservesExistingProviders() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val provider = provider()
    val repository =
      FakeRepository(
        loadResults = listOf(Result.success(listOf(provider))),
        createResult = Result.failure(IllegalStateException("Invalid url")),
      )
    val viewModel = CustomMetadataProviderManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateName("Community"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateUrl("not-a-url"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SubmitCreate)
    advanceUntilIdle()

    assertEquals(listOf(provider), viewModel.uiState.value.providers)
    val failure =
      viewModel.uiState.value.apiState as CustomMetadataProviderManagementApiState.Failure
    assertEquals("Invalid url", failure.message)
    collection.cancel()
  }

  @Test
  fun createMutation_blocksOverlappingCreateAndDelete() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val provider = provider()
    val createResult = CompletableDeferred<Result<CustomMetadataProvider>>()
    val repository =
      BlockingMutationRepository(
        loadResults = listOf(Result.success(emptyList()), Result.success(listOf(provider))),
        createResult = createResult,
      )
    val viewModel = CustomMetadataProviderManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateName("Community"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateUrl("https://provider.example"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SubmitCreate)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isMutating)

    viewModel.onEvent(CustomMetadataProviderManagementEvent.SubmitCreate)
    viewModel.onEvent(CustomMetadataProviderManagementEvent.BeginDelete(provider))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.ConfirmDelete)
    assertEquals(1, repository.createCalls)
    assertEquals(0, repository.deleteCalls)

    createResult.complete(Result.success(provider))
    advanceUntilIdle()
    assertEquals(listOf(provider), viewModel.uiState.value.providers)
    collection.cancel()
  }

  @Test
  fun deleteMutation_blocksOverlappingDeleteAndCreate() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val provider = provider()
    val deleteResult = CompletableDeferred<Result<Unit>>()
    val repository =
      BlockingMutationRepository(
        loadResults = listOf(Result.success(listOf(provider)), Result.success(emptyList())),
        deleteResult = deleteResult,
      )
    val viewModel = CustomMetadataProviderManagementViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(CustomMetadataProviderManagementEvent.BeginDelete(provider))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.ConfirmDelete)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isMutating)

    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateName("Community"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.UpdateUrl("https://provider.example"))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.SubmitCreate)
    viewModel.onEvent(CustomMetadataProviderManagementEvent.BeginDelete(provider))
    viewModel.onEvent(CustomMetadataProviderManagementEvent.ConfirmDelete)
    assertEquals(0, repository.createCalls)
    assertEquals(1, repository.deleteCalls)

    deleteResult.complete(Result.success(Unit))
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.providers.isEmpty())
    collection.cancel()
  }

  private fun provider(authHeaderValue: String? = null) =
    CustomMetadataProvider(
      id = "provider-1",
      name = "Community",
      url = "https://provider.example",
      slug = "custom-provider-1",
      authHeaderValue = authHeaderValue,
    )

  private fun TestScope.collectState(
    viewModel: CustomMetadataProviderManagementViewModel
  ): Job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }

  private class FakeRepository(
    private val loadResults: List<Result<List<CustomMetadataProvider>>>,
    private val createResult: Result<CustomMetadataProvider> =
      Result.success(
        CustomMetadataProvider(
          id = "provider-1",
          name = "Community",
          url = "https://provider.example",
          slug = "custom-provider-1",
        )
      ),
    private val deleteResult: Result<Unit> = Result.success(Unit),
  ) : MetadataUtilitiesRepositoryContract {
    private val pendingLoads = ArrayDeque(loadResults)
    var loadCalls = 0
      private set
    var deleteCalls = 0
      private set
    var createdAuthHeader: String? = null
      private set

    override suspend fun loadTags(): Result<List<String>> = Result.success(emptyList())

    override suspend fun renameTag(
      tag: String,
      newTag: String,
    ): Result<dev.halim.shelfdroid.core.data.metadata.TagMutation> =
      Result.success(dev.halim.shelfdroid.core.data.metadata.TagMutation(0))

    override suspend fun deleteTag(
      tag: String
    ): Result<dev.halim.shelfdroid.core.data.metadata.TagMutation> =
      Result.success(dev.halim.shelfdroid.core.data.metadata.TagMutation(0))

    override suspend fun loadCustomMetadataProviders(): Result<List<CustomMetadataProvider>> {
      loadCalls += 1
      return pendingLoads.removeFirst()
    }

    override suspend fun createCustomMetadataProvider(
      name: String,
      url: String,
      authHeaderValue: String?,
    ): Result<CustomMetadataProvider> {
      createdAuthHeader = authHeaderValue
      return createResult
    }

    override suspend fun deleteCustomMetadataProvider(providerId: String): Result<Unit> {
      deleteCalls += 1
      return deleteResult
    }
  }

  private class BlockingMutationRepository(
    private val loadResults: List<Result<List<CustomMetadataProvider>>>,
    private val createResult: CompletableDeferred<Result<CustomMetadataProvider>>? = null,
    private val deleteResult: CompletableDeferred<Result<Unit>>? = null,
  ) : MetadataUtilitiesRepositoryContract {
    private val pendingLoads = ArrayDeque(loadResults)
    var createCalls = 0
      private set
    var deleteCalls = 0
      private set

    override suspend fun loadTags(): Result<List<String>> = Result.success(emptyList())

    override suspend fun renameTag(
      tag: String,
      newTag: String,
    ): Result<dev.halim.shelfdroid.core.data.metadata.TagMutation> =
      Result.success(dev.halim.shelfdroid.core.data.metadata.TagMutation(0))

    override suspend fun deleteTag(
      tag: String
    ): Result<dev.halim.shelfdroid.core.data.metadata.TagMutation> =
      Result.success(dev.halim.shelfdroid.core.data.metadata.TagMutation(0))

    override suspend fun loadCustomMetadataProviders(): Result<List<CustomMetadataProvider>> =
      pendingLoads.removeFirst()

    override suspend fun createCustomMetadataProvider(
      name: String,
      url: String,
      authHeaderValue: String?,
    ): Result<CustomMetadataProvider> {
      createCalls += 1
      return createResult!!.await()
    }

    override suspend fun deleteCustomMetadataProvider(providerId: String): Result<Unit> {
      deleteCalls += 1
      return deleteResult!!.await()
    }
  }
}
