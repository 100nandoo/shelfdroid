package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateResult
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleInterval
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleMode
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleValidationException
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationScheduleValidationState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystem
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProvider
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProviderState
import java.util.ArrayDeque
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryAdministrationCreateViewModelTest {

  @Test
  fun providerFailure_isRetryableAndPreventsSubmission() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(
            listOf(
              Result.failure(IllegalStateException("offline")),
              Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.providerState is LibraryAdministrationProviderState.Failure)
    assertEquals(
      null,
      (viewModel.uiState.value.providerState as LibraryAdministrationProviderState.Failure).message,
    )
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.validation.errors.containsKey(LibraryAdministrationCreateField.PROVIDER))
    assertEquals(0, repository.createCalls)

    viewModel.onEvent(LibraryAdministrationCreateEvent.RetryProviders)
    advanceUntilIdle()
    assertEquals(2, repository.providerCalls)
    collection.cancel()
  }

  @Test
  fun mediaTypeChange_preservesHiddenProviderDraft() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(
            listOf(
              Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))),
              Result.success(listOf(LibraryAdministrationProvider("itunes", "iTunes"))),
              Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectMediaType(LibraryAdministrationMediaType.PODCAST))
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectProvider("itunes"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectMediaType(LibraryAdministrationMediaType.BOOK))
    advanceUntilIdle()

    assertEquals("audible", viewModel.uiState.value.draft.bookProvider)
    assertEquals("itunes", viewModel.uiState.value.draft.podcastProvider)
    collection.cancel()
  }

  @Test
  fun invalidSubmitFocusesFirstInvalidFieldAndBackConfirmsDirtyDraft() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    assertEquals(LibraryAdministrationCreateField.NAME, viewModel.uiState.value.focusField)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ConsumeFocus)
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Back)
    assertTrue(viewModel.uiState.value.discardDialog)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ConfirmDiscard)
    assertTrue(viewModel.uiState.value.navigation != null)
    collection.cancel()
  }

  @Test
  fun typingManualFolder_marksDraftDirtyAndBackConfirmsDiscard() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateManualFolder("/new-books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Back)

    assertTrue(viewModel.uiState.value.isDirty)
    assertTrue(viewModel.uiState.value.discardDialog)
    collection.cancel()
  }

  @Test
  fun addingManualFolder_addsOneNormalizedFolderAndClearsInput() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateManualFolder(" C:\\Books\\ "))
    viewModel.onEvent(LibraryAdministrationCreateEvent.AddManualFolder)

    assertEquals(listOf("C:/Books"), viewModel.uiState.value.draft.folders)
    assertEquals("", viewModel.uiState.value.manualFolderDraft)
    collection.cancel()
  }

  @Test
  fun successfulCreateEmitsNavigationAfterRepositoryReconciles() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val library = LibraryAdministrationLibrary("books", "Books", LibraryAdministrationMediaType.BOOK, 1)
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        createResult = Result.success(LibraryAdministrationCreateResult.Created(library)),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectFolder("/books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.navigation is dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateNavigation.Created)
    assertEquals(1, repository.createCalls)
    collection.cancel()
  }

  @Test
  fun filesystemBrowser_exposesDirectoriesAndAddsSelectedFolder() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        filesystemResult =
          Result.success(
            LibraryAdministrationFilesystem(
              isPosix = false,
              directories =
                listOf(
                  dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDirectory(
                    path = "C:/Media",
                    name = "Media",
                    level = 0,
                  )
                ),
            )
          ),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.OpenFilesystem)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.filesystemState is LibraryAdministrationFilesystemState.Success)
    assertEquals(1, repository.browseCalls)
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectFolder("C:\\Media"))
    assertEquals(listOf("C:/Media"), viewModel.uiState.value.draft.folders)
    collection.cancel()
  }

  @Test
  fun serverFailure_isReportedWithoutNavigation() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        createResult = Result.failure(IllegalStateException("server rejected request")),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectFolder("/books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(
      null,
      (viewModel.uiState.value.submissionState as LibraryAdministrationCreateSubmissionState.ServerFailure).message,
    )
    assertEquals(null, viewModel.uiState.value.navigation)
    collection.cancel()
  }

  @Test
  fun localSynchronizationFailure_isRetryableAfterServerCreate() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val library = LibraryAdministrationLibrary("books", "Books", LibraryAdministrationMediaType.BOOK, 1)
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        createResult =
          Result.success(
            LibraryAdministrationCreateResult.CreatedButNotSynchronized(
              library = library,
              error = IllegalStateException("catalog refresh failed"),
            )
          ),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectFolder("/books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.submissionState is LibraryAdministrationCreateSubmissionState.LocalSyncFailure)
    assertEquals(null, viewModel.uiState.value.navigation)
    viewModel.onEvent(LibraryAdministrationCreateEvent.RetryLocalSynchronization)
    advanceUntilIdle()

    assertEquals(1, repository.synchronizeCalls)
    assertTrue(
      viewModel.uiState.value.navigation is
        dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateNavigation.Created
    )
    collection.cancel()
  }

  @Test
  fun settingsAndScannerDraftsRemainHiddenAndIndependentAcrossMediaSwitches() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(
            listOf(
              Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))),
              Result.success(listOf(LibraryAdministrationProvider("itunes", "iTunes"))),
              Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateAudiobooksOnly(true))
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateScriptedEpubs(true))
    viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleMetadataSource("nfoFile", false))
    viewModel.onEvent(
      LibraryAdministrationCreateEvent.SelectMediaType(LibraryAdministrationMediaType.PODCAST)
    )
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdatePodcastSearchRegion("gb"))

    assertTrue(viewModel.uiState.value.draft.bookSettings.audiobooksOnly)
    assertEquals("gb", viewModel.uiState.value.draft.podcastSettings.podcastSearchRegion)
    assertEquals(false, viewModel.uiState.value.draft.metadataSources.first { it.id == "nfoFile" }.enabled)

    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectMediaType(LibraryAdministrationMediaType.BOOK))
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.draft.bookSettings.audiobooksOnly)
    assertEquals(true, viewModel.uiState.value.draft.bookSettings.epubsAllowScriptedContent)
    assertEquals(listOf("folderStructure", "audioMetatags", "txtFiles", "opfFile", "absMetadata"), viewModel.uiState.value.draft.metadataPrecedence)
    collection.cancel()
  }

  @Test
  fun scannerValidationSelectsScannerTab() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectFolder("/books"))
    LibraryAdministrationDraft().metadataSources.forEach { source ->
      viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleMetadataSource(source.id, false))
    }
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)

    assertEquals(
      dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateTab.SCANNER,
      viewModel.uiState.value.selectedTab,
    )
    assertTrue(
      viewModel.uiState.value.validation.errors.containsKey(LibraryAdministrationCreateField.SCANNER_PRECEDENCE)
    )
    collection.cancel()
  }

  @Test
  fun advancedScheduleServerInvalid_showsInlineErrorAndPreventsCreate() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        validationResult = Result.failure(LibraryAdministrationScheduleValidationException.Invalid()),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdministrationCreateEvent.SelectScheduleMode(LibraryAdministrationScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateAdvancedScheduleCron("61 0 * * *"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)
    assertTrue(
      viewModel.uiState.value.scheduleValidation is
        LibraryAdministrationScheduleValidationState.Invalid
    )
    assertTrue(
      viewModel.uiState.value.validation.errors[LibraryAdministrationCreateField.SCHEDULE]!!
        .contains(LibraryAdministrationCreateError.SCHEDULE_INVALID)
    )
    collection.cancel()
  }

  @Test
  fun advancedScheduleValidationUnavailable_isInlineAndRetryable() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        validationResult = Result.failure(LibraryAdministrationScheduleValidationException.Unavailable()),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdministrationCreateEvent.SelectScheduleMode(LibraryAdministrationScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(0, repository.createCalls)
    assertTrue(
      viewModel.uiState.value.scheduleValidation is
        LibraryAdministrationScheduleValidationState.Unavailable
    )
    assertTrue(
      viewModel.uiState.value.validation.errors[LibraryAdministrationCreateField.SCHEDULE]!!
        .contains(LibraryAdministrationCreateError.SCHEDULE_VALIDATION_UNAVAILABLE)
    )
    collection.cancel()
  }

  @Test
  fun explicitAdvancedScheduleValidation_leavesValidStateWithoutCreating() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdministrationCreateEvent.SelectScheduleMode(LibraryAdministrationScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.ValidateSchedule)
    advanceUntilIdle()

    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)
    assertEquals(
      LibraryAdministrationScheduleValidationState.Valid,
      viewModel.uiState.value.scheduleValidation,
    )
    collection.cancel()
  }

  @Test
  fun submitWhileAdvancedScheduleValidationIsInFlight_runsValidationAndCreateOnce() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val validationGate = CompletableDeferred<Result<Unit>>()
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        createResult =
          Result.success(
            LibraryAdministrationCreateResult.Created(
              LibraryAdministrationLibrary("books", "Books", LibraryAdministrationMediaType.BOOK, 1)
            )
          ),
        validationGate = validationGate,
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdministrationCreateEvent.SelectScheduleMode(LibraryAdministrationScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)
    assertEquals(
      LibraryAdministrationScheduleValidationState.Validating,
      viewModel.uiState.value.scheduleValidation,
    )

    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)

    validationGate.complete(Result.success(Unit))
    advanceUntilIdle()
    assertEquals(1, repository.validationCalls)
    assertEquals(1, repository.createCalls)
    collection.cancel()
  }

  @Test
  fun simpleSchedulePresetSkipsServerValidationAndCreatesActiveCron() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdministrationProvider("audible", "Audible"))))),
        createResult = Result.success(LibraryAdministrationCreateResult.Created(
          LibraryAdministrationLibrary("books", "Books", LibraryAdministrationMediaType.BOOK, 1)
        )),
      )
    val viewModel = LibraryAdministrationCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdministrationCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdministrationCreateEvent.SelectScheduleInterval(
        LibraryAdministrationScheduleInterval.Every15Minutes
      )
    )
    viewModel.onEvent(LibraryAdministrationCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(0, repository.validationCalls)
    assertEquals(1, repository.createCalls)
    assertEquals("*/15 * * * *", repository.createdDraft?.schedule?.cronExpression)
    collection.cancel()
  }

  private fun prepareValidDraft(viewModel: LibraryAdministrationCreateViewModel) {
    viewModel.onEvent(LibraryAdministrationCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdministrationCreateEvent.SelectFolder("/books"))
  }

  private fun kotlinx.coroutines.test.TestScope.collectState(
    viewModel: LibraryAdministrationCreateViewModel
  ): Job = backgroundScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
    viewModel.uiState.collect {}
  }

  private class FakeRepository(
    providerResults: ArrayDeque<Result<List<LibraryAdministrationProvider>>>,
    private val filesystemResult: Result<LibraryAdministrationFilesystem> =
      Result.success(LibraryAdministrationFilesystem(true, emptyList())),
    private val createResult: Result<LibraryAdministrationCreateResult> =
      Result.failure(IllegalStateException("not configured")),
    private val validationResult: Result<Unit> = Result.success(Unit),
    private val validationGate: CompletableDeferred<Result<Unit>>? = null,
  ) : LibraryAdministrationCreateContract {
    private val providers = providerResults
    var providerCalls = 0
    var createCalls = 0
    var browseCalls = 0
    var synchronizeCalls = 0
    var validationCalls = 0
    var createdDraft: LibraryAdministrationDraft? = null

    override suspend fun loadLibraryProviders(
      mediaType: LibraryAdministrationMediaType
    ): Result<List<LibraryAdministrationProvider>> {
      providerCalls++
      return providers.removeFirst()
    }

    override suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdministrationFilesystem> =
      filesystemResult.also { browseCalls++ }

    override suspend fun createLibrary(
      draft: LibraryAdministrationDraft
    ): Result<LibraryAdministrationCreateResult> {
      createCalls++
      createdDraft = draft
      return createResult
    }

    override suspend fun validateLibrarySchedule(expression: String): Result<Unit> {
      validationCalls++
      return validationGate?.await() ?: validationResult
    }

    override suspend fun synchronizeLibraries(): Result<Unit> {
      synchronizeCalls++
      return Result.success(Unit)
    }
  }
}
