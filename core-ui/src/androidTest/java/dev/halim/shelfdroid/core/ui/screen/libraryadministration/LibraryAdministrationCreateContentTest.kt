package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystem
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationBookSettings
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProvider
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationValidation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryAdministrationCreateContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun details_supportBothMediaTypesProviderFoldersAndPersistentSubmit() {
    composeRule.setContent {
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            draft =
              LibraryAdministrationDraft(
                mediaType = LibraryAdministrationMediaType.BOOK,
                name = "Books",
                folders = listOf("/media/books"),
                bookProvider = "audible",
              ),
            providerState =
              LibraryAdministrationProviderState.Success(
                listOf(LibraryAdministrationProvider("audible", "Audible"))
              ),
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
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            providerState =
              LibraryAdministrationProviderState.Failure("internal provider exception")
          )
      )
    }

    composeRule.onNodeWithText("Could not load providers.").assertIsDisplayed()
    composeRule.onAllNodesWithText("internal provider exception").assertCountEquals(0)
    composeRule.onNodeWithText("Retry").assertIsDisplayed()
    composeRule.onAllNodesWithText("Create Library").assertCountEquals(2)
  }

  @Test
  fun invalidFields_exposeInlineErrors() {
    composeRule.setContent {
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            validation =
              LibraryAdministrationValidation(
                errors =
                  mapOf(
                    LibraryAdministrationCreateField.NAME to
                      listOf(LibraryAdministrationCreateError.NAME_REQUIRED),
                    LibraryAdministrationCreateField.FOLDERS to
                      listOf(LibraryAdministrationCreateError.FOLDERS_REQUIRED),
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
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            filesystemState =
              LibraryAdministrationFilesystemState.Success(
                path = null,
                filesystem =
                  LibraryAdministrationFilesystem(
                    isPosix = true,
                    directories =
                      List(40) { index ->
                        dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDirectory(
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

    composeRule
      .onNodeWithContentDescription("Filesystem directories")
      .assert(hasScrollAction())
  }

  @Test
  fun bookSettings_exposeApplicableOptionsAndScriptedEpubWarning() {
    composeRule.setContent {
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            selectedTab = LibraryAdministrationCreateTab.SETTINGS,
            draft =
              LibraryAdministrationDraft(
                bookSettings = LibraryAdministrationBookSettings(epubsAllowScriptedContent = true)
              ),
          )
      )
    }

    composeRule.onNodeWithText("Use square covers").assertIsDisplayed()
    composeRule.onNodeWithText("Audiobooks only").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Skip matching books with ASIN").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Allow scripted EPUB content").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithText("Scripted EPUB content can execute active code from an ebook. Only enable this for trusted files.")
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun podcastSettings_omitScannerTab() {
    composeRule.setContent {
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            selectedTab = LibraryAdministrationCreateTab.SETTINGS,
            draft = LibraryAdministrationDraft(mediaType = LibraryAdministrationMediaType.PODCAST),
          )
      )
    }

    composeRule.onNodeWithText("Podcast search region").assertIsDisplayed()
    composeRule.onAllNodesWithText("Scanner").assertCountEquals(0)
  }

  @Test
  fun scanner_exposesAllSixSourcesAndAccessibleReorderActions() {
    composeRule.setContent {
      LibraryAdministrationCreateContent(
        uiState =
          LibraryAdministrationCreateUiState(
            selectedTab = LibraryAdministrationCreateTab.SCANNER,
            draft = LibraryAdministrationDraft(),
          )
      )
    }

    composeRule.onNodeWithText("Folder structure").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Audio file meta tags OR ebook metadata").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("NFO file").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("desc.txt & reader.txt files").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("OPF file").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Audiobookshelf metadata file").performScrollTo().assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Audiobookshelf metadata file, priority 1")
      .assertIsDisplayed()
    composeRule
      .onNodeWithContentDescription("Folder structure, priority 6")
      .assertIsDisplayed()
    composeRule.onAllNodesWithText("Move up").assertCountEquals(6)
  }
}
