package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateContract
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateResult
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleInterval
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleMode
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleValidationException
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleValidationState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminFilesystem
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminProvider
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminProviderState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryAdminCreateViewModelTest {

  @Test
  fun providerFailure_isRetryableAndPreventsSubmission() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(
            listOf(
              Result.failure(IllegalStateException("offline")),
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.providerState is LibraryAdminProviderState.Failure)
    assertEquals(
      null,
      (viewModel.uiState.value.providerState as LibraryAdminProviderState.Failure).message,
    )
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.validation.errors.containsKey(LibraryAdminCreateField.PROVIDER))
    assertEquals(LibraryAdminCreateTab.DETAILS, viewModel.uiState.value.selectedTab)
    assertEquals(LibraryAdminCreateField.PROVIDER, viewModel.uiState.value.focusField)
    assertEquals(0, repository.createCalls)

    viewModel.onEvent(LibraryAdminCreateEvent.RetryProviders)
    advanceUntilIdle()
    assertEquals(2, repository.providerCalls)
    assertTrue(viewModel.uiState.value.providerState is LibraryAdminProviderState.Success)
    assertTrue(
      viewModel.uiState.value.validation.errors.none { it.key == LibraryAdminCreateField.PROVIDER }
    )
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
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
              Result.success(listOf(LibraryAdminProvider("itunes", "iTunes"))),
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.PODCAST))
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminCreateEvent.SelectProvider("itunes"))
    viewModel.onEvent(LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.BOOK))
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
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    assertEquals(LibraryAdminCreateField.NAME, viewModel.uiState.value.focusField)
    viewModel.onEvent(LibraryAdminCreateEvent.ConsumeFocus)
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdminCreateEvent.Back)
    assertTrue(viewModel.uiState.value.discardDialog)
    viewModel.onEvent(LibraryAdminCreateEvent.ConfirmDiscard)
    assertTrue(viewModel.uiState.value.navigation != null)
    collection.cancel()
  }

  @Test
  fun typingManualFolder_marksDraftDirtyAndBackConfirmsDiscard() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.UpdateManualFolder("/new-books"))
    viewModel.onEvent(LibraryAdminCreateEvent.Back)

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
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.UpdateManualFolder(" C:\\Books\\ "))
    viewModel.onEvent(LibraryAdminCreateEvent.AddManualFolder)

    assertEquals(listOf("C:/Books"), viewModel.uiState.value.draft.folders)
    assertEquals("", viewModel.uiState.value.manualFolderDraft)
    collection.cancel()
  }

  @Test
  fun successfulCreateEmitsNavigationAfterRepositoryReconciles() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val library = LibraryAdminLibrary("books", "Books", LibraryAdminMediaType.BOOK, 1)
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        createResult = Result.success(LibraryAdminCreateResult.Created(library)),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdminCreateEvent.SelectFolder("/books"))
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.navigation is dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateNavigation.Created)
    assertEquals(1, repository.createCalls)
    collection.cancel()
  }

  @Test
  fun filesystemBrowser_exposesDirectoriesAndAddsSelectedFolder() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        filesystemResult =
          Result.success(
            LibraryAdminFilesystem(
              isPosix = false,
              directories =
                listOf(
                  dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminDirectory(
                    path = "C:/Media",
                    name = "Media",
                    level = 0,
                  )
                ),
            )
          ),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.OpenFilesystem)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.filesystemState is LibraryAdminFilesystemState.Success)
    assertEquals(1, repository.browseCalls)
    viewModel.onEvent(LibraryAdminCreateEvent.SelectFolder("C:\\Media"))
    assertEquals(listOf("C:/Media"), viewModel.uiState.value.draft.folders)
    collection.cancel()
  }

  @Test
  fun serverFailure_isReportedWithoutNavigation() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        createResult = Result.failure(IllegalStateException("server rejected request")),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdminCreateEvent.SelectFolder("/books"))
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(
      null,
      (viewModel.uiState.value.submissionState as LibraryAdminCreateSubmissionState.ServerFailure).message,
    )
    assertEquals(null, viewModel.uiState.value.navigation)
    collection.cancel()
  }

  @Test
  fun localSynchronizationFailure_isRetryableAfterServerCreate() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val library = LibraryAdminLibrary("books", "Books", LibraryAdminMediaType.BOOK, 1)
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        createResult =
          Result.success(
            LibraryAdminCreateResult.CreatedButNotSynchronized(
              library = library,
              error = IllegalStateException("catalog refresh failed"),
            )
          ),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdminCreateEvent.SelectFolder("/books"))
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.submissionState is LibraryAdminCreateSubmissionState.LocalSyncFailure)
    assertEquals(null, viewModel.uiState.value.navigation)
    viewModel.onEvent(LibraryAdminCreateEvent.RetryLocalSynchronization)
    advanceUntilIdle()

    assertEquals(1, repository.synchronizeCalls)
    assertTrue(
      viewModel.uiState.value.navigation is
        dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateNavigation.Created
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
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
              Result.success(listOf(LibraryAdminProvider("itunes", "iTunes"))),
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.UpdateAudiobooksOnly(true))
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateScriptedEpubs(true))
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleMetadataSource("nfoFile", false))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.PODCAST)
    )
    advanceUntilIdle()
    viewModel.onEvent(LibraryAdminCreateEvent.UpdatePodcastSearchRegion("gb"))

    assertTrue(viewModel.uiState.value.draft.bookSettings.audiobooksOnly)
    assertEquals("gb", viewModel.uiState.value.draft.podcastSettings.podcastSearchRegion)
    assertEquals(false, viewModel.uiState.value.draft.metadataSources.first { it.id == "nfoFile" }.enabled)

    viewModel.onEvent(LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.BOOK))
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.draft.bookSettings.audiobooksOnly)
    assertEquals(true, viewModel.uiState.value.draft.bookSettings.epubsAllowScriptedContent)
    assertEquals(listOf("folderStructure", "audioMetatags", "txtFiles", "opfFile", "absMetadata"), viewModel.uiState.value.draft.metadataPrecedence)
    collection.cancel()
  }

  @Test
  fun finishThresholdModeAndValueChangesUseIndependentBookAndPodcastDrafts() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(
            listOf(
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
              Result.success(listOf(LibraryAdminProvider("itunes", "iTunes"))),
              Result.success(listOf(LibraryAdminProvider("audible", "Audible"))),
            )
          )
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectFinishThresholdMode(
        LibraryAdminFinishThresholdMode.PERCENT_COMPLETE
      )
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateFinishThresholdValue(75))
    assertEquals(75, viewModel.uiState.value.draft.bookSettings.markAsFinishedPercentComplete)
    assertNull(viewModel.uiState.value.draft.bookSettings.markAsFinishedTimeRemaining)

    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.PODCAST)
    )
    advanceUntilIdle()
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectFinishThresholdMode(
        LibraryAdminFinishThresholdMode.PERCENT_COMPLETE
      )
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateFinishThresholdValue(55))
    assertEquals(55, viewModel.uiState.value.draft.podcastSettings.markAsFinishedPercentComplete)
    assertNull(viewModel.uiState.value.draft.podcastSettings.markAsFinishedTimeRemaining)

    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.BOOK)
    )
    advanceUntilIdle()
    assertEquals(75, viewModel.uiState.value.draft.bookSettings.markAsFinishedPercentComplete)
    assertNull(viewModel.uiState.value.draft.bookSettings.markAsFinishedTimeRemaining)
    assertEquals(55, viewModel.uiState.value.draft.podcastSettings.markAsFinishedPercentComplete)
    collection.cancel()
  }

  @Test
  fun draftMutationClearsValidationAndInvalidatesScheduleValidation() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdminCreateEvent.ValidateSchedule)
    advanceUntilIdle()
    assertEquals(
      LibraryAdminScheduleValidationState.Valid,
      viewModel.uiState.value.scheduleValidation,
    )

    viewModel.onEvent(LibraryAdminCreateEvent.UpdateCoverAspectRatio(0))

    assertTrue(viewModel.uiState.value.isDirty)
    assertTrue(viewModel.uiState.value.validation.errors.isEmpty())
    assertEquals(
      LibraryAdminScheduleValidationState.Idle,
      viewModel.uiState.value.scheduleValidation,
    )
    collection.cancel()
  }

  @Test
  fun scannerValidationSelectsScannerTab() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()

    viewModel.onEvent(LibraryAdminCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdminCreateEvent.SelectFolder("/books"))
    LibraryAdminDraft().metadataSources.forEach { source ->
      viewModel.onEvent(LibraryAdminCreateEvent.ToggleMetadataSource(source.id, false))
    }
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)

    assertEquals(
      dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateTab.SCANNER,
      viewModel.uiState.value.selectedTab,
    )
    assertTrue(
      viewModel.uiState.value.validation.errors.containsKey(LibraryAdminCreateField.SCANNER_PRECEDENCE)
    )
    collection.cancel()
  }

  @Test
  fun advancedScheduleServerInvalid_showsInlineErrorAndPreventsCreate() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        validationResult = Result.failure(LibraryAdminScheduleValidationException.Invalid()),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron("61 0 * * *"))
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)
    assertTrue(
      viewModel.uiState.value.scheduleValidation is
        LibraryAdminScheduleValidationState.Invalid
    )
    assertTrue(
      viewModel.uiState.value.validation.errors[LibraryAdminCreateField.SCHEDULE]!!
        .contains(LibraryAdminCreateError.SCHEDULE_INVALID)
    )
    collection.cancel()
  }

  @Test
  fun advancedScheduleValidationUnavailable_isInlineAndRetryable() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        validationResult = Result.failure(LibraryAdminScheduleValidationException.Unavailable()),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(0, repository.createCalls)
    assertTrue(
      viewModel.uiState.value.scheduleValidation is
        LibraryAdminScheduleValidationState.Unavailable
    )
    assertTrue(
      viewModel.uiState.value.validation.errors[LibraryAdminCreateField.SCHEDULE]!!
        .contains(LibraryAdminCreateError.SCHEDULE_VALIDATION_UNAVAILABLE)
    )
    collection.cancel()
  }

  @Test
  fun explicitAdvancedScheduleValidation_leavesValidStateWithoutCreating() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository =
      FakeRepository(
        providerResults =
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible")))))
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdminCreateEvent.ValidateSchedule)
    advanceUntilIdle()

    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)
    assertEquals(
      LibraryAdminScheduleValidationState.Valid,
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
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        createResult =
          Result.success(
            LibraryAdminCreateResult.Created(
              LibraryAdminLibrary("books", "Books", LibraryAdminMediaType.BOOK, 1)
            )
          ),
        validationGate = validationGate,
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced)
    )
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron("0 0 * * 1"))
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    assertEquals(1, repository.validationCalls)
    assertEquals(0, repository.createCalls)
    assertEquals(
      LibraryAdminScheduleValidationState.Validating,
      viewModel.uiState.value.scheduleValidation,
    )

    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
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
          ArrayDeque(listOf(Result.success(listOf(LibraryAdminProvider("audible", "Audible"))))),
        createResult = Result.success(LibraryAdminCreateResult.Created(
          LibraryAdminLibrary("books", "Books", LibraryAdminMediaType.BOOK, 1)
        )),
      )
    val viewModel = LibraryAdminCreateViewModel(repository)
    val collection = collectState(viewModel)
    advanceUntilIdle()
    prepareValidDraft(viewModel)
    viewModel.onEvent(LibraryAdminCreateEvent.ToggleSchedule(true))
    viewModel.onEvent(
      LibraryAdminCreateEvent.SelectScheduleInterval(
        LibraryAdminScheduleInterval.Every15Minutes
      )
    )
    viewModel.onEvent(LibraryAdminCreateEvent.Submit)
    advanceUntilIdle()

    assertEquals(0, repository.validationCalls)
    assertEquals(1, repository.createCalls)
    assertEquals("*/15 * * * *", repository.createdDraft?.schedule?.cronExpression)
    collection.cancel()
  }

  private fun prepareValidDraft(viewModel: LibraryAdminCreateViewModel) {
    viewModel.onEvent(LibraryAdminCreateEvent.UpdateName("Books"))
    viewModel.onEvent(LibraryAdminCreateEvent.SelectFolder("/books"))
  }

  private fun kotlinx.coroutines.test.TestScope.collectState(
    viewModel: LibraryAdminCreateViewModel
  ): Job = backgroundScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
    viewModel.uiState.collect {}
  }

  private class FakeRepository(
    providerResults: ArrayDeque<Result<List<LibraryAdminProvider>>>,
    private val filesystemResult: Result<LibraryAdminFilesystem> =
      Result.success(LibraryAdminFilesystem(true, emptyList())),
    private val createResult: Result<LibraryAdminCreateResult> =
      Result.failure(IllegalStateException("not configured")),
    private val validationResult: Result<Unit> = Result.success(Unit),
    private val validationGate: CompletableDeferred<Result<Unit>>? = null,
  ) : LibraryAdminCreateContract {
    private val providers = providerResults
    var providerCalls = 0
    var createCalls = 0
    var browseCalls = 0
    var synchronizeCalls = 0
    var validationCalls = 0
    var createdDraft: LibraryAdminDraft? = null

    override suspend fun loadLibraryProviders(
      mediaType: LibraryAdminMediaType
    ): Result<List<LibraryAdminProvider>> {
      providerCalls++
      return providers.removeFirst()
    }

    override suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdminFilesystem> =
      filesystemResult.also { browseCalls++ }

    override suspend fun createLibrary(
      draft: LibraryAdminDraft
    ): Result<LibraryAdminCreateResult> {
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
