package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationConnectionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationTaskState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationError
import dev.halim.shelfdroid.core.data.task.ServerTask
import dev.halim.shelfdroid.core.data.task.ServerTaskResult
import dev.halim.shelfdroid.core.data.task.ServerTaskStatus
import dev.halim.shelfdroid.core.data.task.ServerTaskSyncState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryAdministrationContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun loadingState_isVisibleAndRefreshIsAccessible() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState = LibraryAdministrationUiState(),
      )
    }

    composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Refresh Libraries").assertIsDisplayed()
  }

  @Test
  fun failureState_showsRetryAndDoesNotShowLibraryNames() {
    var refreshes = 0
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Failure("offline"),
            isRefreshing = false,
          ),
        onEvent = { refreshes += 1 },
      )
    }

    composeRule.onNodeWithText("offline").assertIsDisplayed()
    composeRule.onNodeWithText("Retry").performClick()
    composeRule.onAllNodesWithText("Books").assertCountEquals(0)
    assertEquals(1, refreshes)
  }

  @Test
  fun emptyState_isVisible() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState = LibraryAdministrationUiState(state = GenericState.Success, isRefreshing = false)
      )
    }

    composeRule.onNodeWithText("No Libraries are available.").assertIsDisplayed()
  }

  @Test
  fun orderedLibraries_showTypeAndIdentityAndRowsAreInert() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "podcasts",
                  name = "Podcasts",
                  mediaType = LibraryAdministrationMediaType.PODCAST,
                  displayOrder = 0,
                ),
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                ),
              ),
          )
      )
    }

    composeRule.onNodeWithText("Podcasts").assertIsDisplayed().assertHasNoClickAction()
    composeRule.onNodeWithText("Podcast Library").assertIsDisplayed()
    composeRule.onNodeWithText("Library ID: podcasts").assertIsDisplayed()
    composeRule.onNodeWithText("Books").assertIsDisplayed().assertHasNoClickAction()
    composeRule.onNodeWithText("Book Library").assertIsDisplayed()
    composeRule.onNodeWithText("Library ID: books").assertIsDisplayed()
  }

  @Test
  fun reorderActions_areAccessibleAndRespectBoundaries() {
    val events = mutableListOf<LibraryAdministrationEvent>()
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            connectionState = LibraryAdministrationConnectionState.CONNECTED,
            taskStates =
              mapOf(
                "books" to LibraryAdministrationTaskState.IDLE,
                "podcasts" to LibraryAdministrationTaskState.IDLE,
              ),
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                ),
                LibraryAdministrationLibrary(
                  id = "podcasts",
                  name = "Podcasts",
                  mediaType = LibraryAdministrationMediaType.PODCAST,
                  displayOrder = 2,
                ),
              ),
          ),
        onEvent = events::add,
      )
    }

    composeRule.onAllNodesWithText("Move down").get(0).performClick()
    assertEquals(LibraryAdministrationEvent.MoveLibrary("books", 1), events.single())
    composeRule.onAllNodesWithText("Move up").assertCountEquals(2)
  }

  @Test
  fun draggingLibraryRow_emitsMoveLibraryEvent() {
    val events = mutableListOf<LibraryAdministrationEvent>()
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            connectionState = LibraryAdministrationConnectionState.CONNECTED,
            taskStates =
              mapOf(
                "books" to LibraryAdministrationTaskState.IDLE,
                "podcasts" to LibraryAdministrationTaskState.IDLE,
              ),
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                ),
                LibraryAdministrationLibrary(
                  id = "podcasts",
                  name = "Podcasts",
                  mediaType = LibraryAdministrationMediaType.PODCAST,
                  displayOrder = 2,
                ),
              ),
          ),
        onEvent = events::add,
      )
    }

    composeRule.onNodeWithText("Books").performTouchInput {
      down(center)
      advanceEventTime(700)
      moveBy(Offset(0f, 64f))
      up()
    }

    assertEquals(listOf(LibraryAdministrationEvent.MoveLibrary("books", 1)), events)
  }

  @Test
  fun scanActions_areEnabledOnlyForKnownIdleTasks() {
    val events = mutableListOf<LibraryAdministrationEvent>()
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            connectionState = LibraryAdministrationConnectionState.CONNECTED,
            taskStates =
              mapOf(
                "books" to LibraryAdministrationTaskState.IDLE,
                "podcasts" to LibraryAdministrationTaskState.ACTIVE,
              ),
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                ),
                LibraryAdministrationLibrary(
                  id = "podcasts",
                  name = "Podcasts",
                  mediaType = LibraryAdministrationMediaType.PODCAST,
                  displayOrder = 2,
                ),
              ),
          ),
        onEvent = events::add,
      )
    }

    composeRule.onAllNodesWithContentDescription("Scan Library").get(0)
      .assertIsEnabled()
      .performClick()
    composeRule.onAllNodesWithContentDescription("Scan Library").get(1).assertIsNotEnabled()
    assertEquals(listOf(LibraryAdministrationEvent.StartScan("books")), events)
  }

  @Test
  fun matchAction_isShownOnlyForBookLibrariesAndUsesDirectEvent() {
    val events = mutableListOf<LibraryAdministrationEvent>()
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            connectionState = LibraryAdministrationConnectionState.CONNECTED,
            taskStates =
              mapOf(
                "books" to LibraryAdministrationTaskState.IDLE,
                "podcasts" to LibraryAdministrationTaskState.IDLE,
              ),
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                ),
                LibraryAdministrationLibrary(
                  id = "podcasts",
                  name = "Podcasts",
                  mediaType = LibraryAdministrationMediaType.PODCAST,
                  displayOrder = 2,
                ),
              ),
          ),
        onEvent = events::add,
      )
    }

    composeRule.onAllNodesWithContentDescription("Match Book metadata").assertCountEquals(1)
    composeRule.onNodeWithContentDescription("Match Book metadata").assertIsEnabled().performClick()
    assertEquals(listOf(LibraryAdministrationEvent.StartMatch("books")), events)
  }

  @Test
  fun completedTask_displaysProgressAndOffersSynchronizationRetry() {
    val events = mutableListOf<LibraryAdministrationEvent>()
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            connectionState = LibraryAdministrationConnectionState.CONNECTED,
            taskStates = mapOf("books" to LibraryAdministrationTaskState.IDLE),
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                )
              ),
            tasks =
              listOf(
                ServerTask(
                  id = "scan",
                  action = "library-scan",
                  libraryId = "books",
                  status = ServerTaskStatus.COMPLETED,
                  result = ServerTaskResult(2, 3, 1, 4_500),
                  syncState = ServerTaskSyncState.FAILED,
                )
              ),
          ),
        onEvent = events::add,
      )
    }

    composeRule.onNodeWithText("Library scan completed").assertIsDisplayed()
    composeRule.onNodeWithText("Added 2, updated 3, missing 1").assertIsDisplayed()
    composeRule.onNodeWithText("Elapsed: 4 seconds").assertIsDisplayed()
    composeRule.onNodeWithText("Library data synchronization failed.").assertIsDisplayed()
    composeRule.onNodeWithText("Retry synchronization").performClick()
    assertEquals(
      listOf(LibraryAdministrationEvent.RetryTaskSynchronization("scan")),
      events,
    )
  }

  @Test
  fun completedMatch_displaysMatchResultsAndSynchronizationRetry() {
    val events = mutableListOf<LibraryAdministrationEvent>()
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            connectionState = LibraryAdministrationConnectionState.CONNECTED,
            taskStates = mapOf("books" to LibraryAdministrationTaskState.IDLE),
            libraries =
              listOf(
                LibraryAdministrationLibrary(
                  id = "books",
                  name = "Books",
                  mediaType = LibraryAdministrationMediaType.BOOK,
                  displayOrder = 1,
                )
              ),
            tasks =
              listOf(
                ServerTask(
                  id = "match",
                  action = "library-match-all",
                  libraryId = "books",
                  status = ServerTaskStatus.COMPLETED,
                  result = ServerTaskResult(updated = 4, elapsedMillis = 4_500),
                  syncState = ServerTaskSyncState.FAILED,
                )
              ),
          ),
        onEvent = events::add,
      )
    }

    composeRule.onNodeWithText("Book metadata matching completed").assertIsDisplayed()
    composeRule.onNodeWithText("Matched or updated 4 books").assertIsDisplayed()
    composeRule.onNodeWithText("Elapsed: 4 seconds").assertIsDisplayed()
    composeRule.onNodeWithText("Library data synchronization failed.").assertIsDisplayed()
    composeRule.onNodeWithText("Retry synchronization").performClick()
    assertEquals(listOf(LibraryAdministrationEvent.RetryTaskSynchronization("match")), events)
  }

  @Test
  fun genericFailures_resolveThroughLocalizedResources() {
    composeRule.setContent {
      LibraryAdministrationContent(
        uiState =
          LibraryAdministrationUiState(
            state = GenericState.Success,
            isRefreshing = false,
            scanError = LibraryAdministrationError.GenericScanStart,
            matchError = LibraryAdministrationError.GenericMatchStart,
            taskSyncError = LibraryAdministrationError.GenericSynchronization,
          )
      )
    }

    composeRule.onNodeWithText("The library scan could not be started.").assertIsDisplayed()
    composeRule.onNodeWithText("Book metadata matching could not be started.").assertIsDisplayed()
    composeRule.onNodeWithText("Library data synchronization failed.").assertIsDisplayed()
  }
}
