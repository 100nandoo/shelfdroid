package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminBookSettings
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystem
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProvider
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminValidation
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
  fun scanner_exposesAllSixSourcesAndAccessibleReorderActions() {
    composeRule.setContent {
      LibraryAdminCreateContent(
        uiState =
          LibraryAdminCreateUiState(
            selectedTab = LibraryAdminCreateTab.SCANNER,
            draft = LibraryAdminDraft(),
          )
      )
    }

    composeRule.onNodeWithText("Folder structure").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithText("Audio file meta tags OR ebook metadata")
      .performScrollTo()
      .assertIsDisplayed()
    composeRule.onNodeWithText("NFO file").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("desc.txt & reader.txt files").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("OPF file").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Audiobookshelf metadata file").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Audiobookshelf metadata file, priority 1")
      .assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Folder structure, priority 6").assertIsDisplayed()
    composeRule.onAllNodesWithText("Move up").assertCountEquals(6)
  }
}
