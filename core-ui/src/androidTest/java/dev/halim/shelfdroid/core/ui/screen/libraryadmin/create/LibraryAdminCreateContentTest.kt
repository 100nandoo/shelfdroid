package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminBookSettings
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystem
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProvider
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminValidation
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LIBRARY_ADMIN_SCANNER_LIST_TAG
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LibraryAdminScannerTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryAdminCreateContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun details_supportBothMediaTypesProviderFoldersAndPersistentSubmit() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            draft =
              LibraryAdminDraft(
                mediaType = MediaType.BOOK,
                name = "Books",
                folders = listOf("/media/books"),
                bookProvider = "audible",
              ),
            providerState =
              LibraryAdminProviderState.Success(listOf(LibraryAdminProvider("audible", "Audible"))),
          )
      )
    }

    composeRule.onNodeWithText("Media type").assertIsDisplayed()
    composeRule.onNodeWithText("Library name").assertIsDisplayed()
    composeRule.onNodeWithText("Audible").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("/media/books").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Details").assertIsDisplayed()
    composeRule.onNodeWithText("Settings").assertIsDisplayed()
    composeRule.onNodeWithText("Scanner").assertIsDisplayed()
    composeRule.onAllNodesWithText("Create Library").assertCountEquals(2)
  }

  @Test
  fun providerFailure_isVisibleWithRetryActionWithoutLeakingInternalMessage() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            providerState = LibraryAdminProviderState.Failure("internal provider exception")
          )
      )
    }

    composeRule.onNodeWithText("Could not load providers.").assertIsDisplayed()
    composeRule.onAllNodesWithText("internal provider exception").assertCountEquals(0)
    composeRule.onNodeWithText("Retry").assertIsDisplayed()
    composeRule.onAllNodesWithText("Create Library").assertCountEquals(2)
  }

  @Test
  fun invalidSubmissionDuringProviderFailure_focusesAccessibleRetryTargetAndRetrySucceeds() {
    var uiState by
      mutableStateOf(
        LibraryAdminCreateUiState(
          draft =
            LibraryAdminDraft(
              name = "Books",
              folders = listOf("/books"),
            ),
          providerState = LibraryAdminProviderState.Failure("internal provider error"),
        )
      )
    val events = mutableListOf<LibraryAdminCreateEvent>()

    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState = uiState,
        onEvent = { event ->
          events += event
          when (event) {
            LibraryAdminCreateEvent.Submit ->
              uiState =
                uiState.copy(
                  selectedTab = LibraryAdminCreateTab.DETAILS,
                  validation =
                    LibraryAdminValidation(
                      errors =
                        mapOf(
                          LibraryAdminCreateField.PROVIDER to
                            listOf(LibraryAdminCreateError.PROVIDER_UNAVAILABLE)
                        )
                    ),
                  focusField = LibraryAdminCreateField.PROVIDER,
                )
            LibraryAdminCreateEvent.ConsumeFocus -> uiState = uiState.copy(focusField = null)
            LibraryAdminCreateEvent.RetryProviders ->
              uiState =
                uiState.copy(
                  draft = uiState.draft.copy(bookProvider = "audible"),
                  providerState =
                    LibraryAdminProviderState.Success(
                      listOf(LibraryAdminProvider("audible", "Audible"))
                    ),
                )
            else -> Unit
          }
        },
      )
    }

    composeRule.onAllNodesWithText("Create Library")[1].performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Could not load providers. Retry").assertIsFocused()

    composeRule.onNodeWithText("Retry").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Audible").assertIsDisplayed()
    assert(events.contains(LibraryAdminCreateEvent.Submit))
    assert(events.contains(LibraryAdminCreateEvent.RetryProviders))
  }

  @Test
  fun providerLoadingAndSuccessStates_exposeFocusableProviderTargets() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            providerState = LibraryAdminProviderState.Loading,
            focusField = LibraryAdminCreateField.PROVIDER,
          )
      )
    }

    composeRule.onNodeWithContentDescription("Loading providers…").assertIsFocused()

    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            draft = LibraryAdminDraft(bookProvider = "audible"),
            providerState =
              LibraryAdminProviderState.Success(listOf(LibraryAdminProvider("audible", "Audible"))),
            focusField = LibraryAdminCreateField.PROVIDER,
          )
      )
    }

    composeRule.onNodeWithText("Audible").assertIsFocused()
  }

  @Test
  fun invalidFields_exposeInlineErrors() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            validation =
              LibraryAdminValidation(
                errors =
                  mapOf(
                    LibraryAdminCreateField.NAME to listOf(LibraryAdminCreateError.NAME_REQUIRED),
                    LibraryAdminCreateField.FOLDERS to
                      listOf(LibraryAdminCreateError.FOLDERS_REQUIRED),
                  )
              )
          )
      )
    }

    composeRule.onNodeWithText("Name is required").assertIsDisplayed()
    composeRule.onNodeWithText("Add at least one folder").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun filesystemDirectoryList_isScrollable() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            filesystemState =
              LibraryAdminFilesystemState.Success(
                path = null,
                filesystem =
                  LibraryAdminFilesystem(
                    isPosix = true,
                    directories =
                      List(40) { index ->
                        dev.halim.shelfdroid.core.data.screen.libraryadmin.create
                          .LibraryAdminDirectory(
                            path = "/media/$index",
                            name = "media-$index",
                            level = 0,
                          )
                      },
                  ),
              )
          )
      )
    }

    composeRule.onNodeWithContentDescription("Filesystem directories").assert(hasScrollAction())
  }

  @Test
  fun bookSettings_exposeApplicableOptionsAndScriptedEpubWarning() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SETTINGS,
            draft =
              LibraryAdminDraft(
                bookSettings = LibraryAdminBookSettings(epubsAllowScriptedContent = true)
              ),
          )
      )
    }

    composeRule.onNodeWithText("Use square covers").assertIsDisplayed()
    composeRule.onNodeWithText("Audiobooks only").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithText("Skip matching books with ASIN")
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithText("Allow scripted EPUB content").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithText(
        "Scripted EPUB content can execute active code from an ebook. Only enable this for trusted files."
      )
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun podcastSettings_omitScannerTab() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SETTINGS,
            draft = LibraryAdminDraft(mediaType = MediaType.PODCAST),
          )
      )
    }

    composeRule.onNodeWithText("Podcast search region").assertIsDisplayed()
    composeRule.onAllNodesWithText("Scanner").assertCountEquals(0)
  }

  @Test
  fun podcastScheduleTab_usesVisibleTabIndexAndRendersSchedule() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCHEDULE,
            draft = LibraryAdminDraft(mediaType = MediaType.PODCAST),
          )
      )
    }

    composeRule.onNodeWithText("Schedule").assertIsDisplayed().assertIsSelected()
    composeRule.onNodeWithText("Automatic library scans").assertIsDisplayed()
    composeRule.onAllNodesWithText("Scanner").assertCountEquals(0)
  }

  @Test
  fun scanner_exposesAllSixSourcesAndReorderHandles() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCANNER,
            draft = LibraryAdminDraft(),
          )
      )
    }

    composeRule.onNodeWithText("Audiobookshelf metadata file").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Audiobookshelf metadata file, priority 1")
      .assertIsDisplayed()
    composeRule.onNodeWithTag(LIBRARY_ADMIN_SCANNER_LIST_TAG).performScrollToIndex(6)
    composeRule.onNodeWithText("Folder structure").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Folder structure, priority 6").assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Reorder Folder structure, position 6 of 6")
      .assertIsDisplayed()
    composeRule.onAllNodesWithText("Move up").assertCountEquals(0)
    composeRule.onAllNodesWithText("Move down").assertCountEquals(0)
  }

  @Test
  fun scannerAccessibilityMove_commitsOneMoveAndUpdatesThePosition() {
    var uiState by
      mutableStateOf(
        LibraryAdminCreateUiState(
          selectedTab = LibraryAdminCreateTab.SCANNER,
          draft = LibraryAdminDraft(),
        )
      )
    val events = mutableListOf<LibraryAdminCreateEvent>()
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState = uiState,
        onEvent = { event ->
          events += event
          if (event is LibraryAdminCreateEvent.MoveMetadataSource) {
            uiState = uiState.copy(draft = uiState.draft.moveMetadataSource(event.id, event.delta))
          }
        },
      )
    }

    val handle =
      composeRule.onNodeWithContentDescription(
        "Reorder Audiobookshelf metadata file, position 1 of 6"
      )
    val actions = handle.fetchSemanticsNode().config[SemanticsActions.CustomActions]
    composeRule.runOnIdle {
      assertTrue(actions.first { it.label == "Move Audiobookshelf metadata file down" }.action())
    }
    composeRule.waitForIdle()

    assertEquals(
      listOf(LibraryAdminCreateEvent.MoveMetadataSource("absMetadata", 1)),
      events,
    )
    composeRule
      .onNodeWithContentDescription("Reorder Audiobookshelf metadata file, position 2 of 6")
      .assertIsDisplayed()
  }

  @Test
  fun scannerDrag_downwardCommitsOneFinalMove() {
    val events = mutableListOf<LibraryAdminCreateEvent>()
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCANNER,
            draft = LibraryAdminDraft(),
          ),
        onEvent = events::add,
      )
    }

    composeRule
      .onNodeWithContentDescription("Reorder Audiobookshelf metadata file, position 1 of 6")
      .performTouchInput {
        swipe(start = center, end = center + Offset(0f, 180f), durationMillis = 200)
      }
    composeRule.waitForIdle()

    assertEquals(1, events.size)
    val event = events.single() as LibraryAdminCreateEvent.MoveMetadataSource
    assertEquals("absMetadata", event.id)
    assertTrue(event.delta > 0)
  }

  @Test
  fun scannerDrag_upwardCommitsOneFinalMove() {
    val events = mutableListOf<LibraryAdminCreateEvent>()
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCANNER,
            draft = LibraryAdminDraft(),
          ),
        onEvent = events::add,
      )
    }
    composeRule.onNodeWithTag(LIBRARY_ADMIN_SCANNER_LIST_TAG).performScrollToIndex(6)

    composeRule
      .onNodeWithContentDescription("Reorder Folder structure, position 6 of 6")
      .performTouchInput {
        swipe(start = center, end = center - Offset(0f, 180f), durationMillis = 200)
      }
    composeRule.waitForIdle()

    assertEquals(1, events.size)
    val event = events.single() as LibraryAdminCreateEvent.MoveMetadataSource
    assertEquals("folderStructure", event.id)
    assertTrue(event.delta < 0)
  }

  @Test
  fun scannerDrag_atBottomEdgeAutoScrollsAcrossOffscreenSources() {
    val events = mutableListOf<LibraryAdminCreateEvent>()
    composeRule.setContent {
      LibraryAdminScannerTab(
        title = "Create Library",
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCANNER,
            draft = LibraryAdminDraft(),
          ),
        onEvent = events::add,
        focusRequester = remember { FocusRequester() },
        listState = rememberLazyListState(),
        modifier = Modifier.height(280.dp),
      )
    }
    val listBottom =
      composeRule
        .onNodeWithTag(LIBRARY_ADMIN_SCANNER_LIST_TAG)
        .fetchSemanticsNode()
        .boundsInRoot
        .bottom
    val handle =
      composeRule.onNodeWithContentDescription(
        "Reorder Audiobookshelf metadata file, position 1 of 6"
      )
    val handleCenterY = handle.fetchSemanticsNode().boundsInRoot.center.y

    composeRule.mainClock.autoAdvance = false
    handle.performTouchInput {
      down(center)
      moveBy(Offset(0f, listBottom - handleCenterY - 2f))
    }
    composeRule.mainClock.advanceTimeBy(1_000)
    composeRule.onNodeWithTag(LIBRARY_ADMIN_SCANNER_LIST_TAG).performTouchInput { up() }
    composeRule.mainClock.autoAdvance = true
    composeRule.waitForIdle()

    val event = events.single() as LibraryAdminCreateEvent.MoveMetadataSource
    assertEquals("absMetadata", event.id)
    assertTrue(event.delta > 1)
  }

  @Test
  fun scannerCancelledDrag_doesNotCommit() {
    val events = mutableListOf<LibraryAdminCreateEvent>()
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCANNER,
            draft = LibraryAdminDraft(),
          ),
        onEvent = events::add,
      )
    }

    composeRule
      .onNodeWithContentDescription("Reorder Audiobookshelf metadata file, position 1 of 6")
      .performTouchInput {
        down(center)
        moveBy(Offset(0f, 96f))
        cancel()
      }
    composeRule.waitForIdle()

    assertEquals(emptyList<LibraryAdminCreateEvent>(), events)
  }

  @Test
  fun scannerDisabledSource_remainsReorderableWhileBusyStateDisablesTheHandle() {
    val disabledFirstSource =
      LibraryAdminDraft().let { draft ->
        draft.copy(
          metadataSources =
            draft.metadataSources.mapIndexed { index, source ->
              if (index == 0) source.copy(enabled = false) else source
            }
        )
      }
    var uiState by
      mutableStateOf(
        LibraryAdminCreateUiState(
          selectedTab = LibraryAdminCreateTab.SCANNER,
          draft = disabledFirstSource,
        )
      )
    composeRule.setContent { LibraryAdminCreateContent(uiState = uiState) }

    composeRule
      .onNodeWithContentDescription("Reorder Audiobookshelf metadata file, position 1 of 6")
      .assertIsEnabled()

    uiState = uiState.copy(submissionState = LibraryAdminCreateSubmissionState.Submitting)
    composeRule.waitForIdle()

    composeRule
      .onNodeWithContentDescription("Reorder Audiobookshelf metadata file, position 1 of 6")
      .assertIsNotEnabled()
  }
}
