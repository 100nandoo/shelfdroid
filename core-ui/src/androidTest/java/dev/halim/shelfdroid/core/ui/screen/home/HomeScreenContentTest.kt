package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun emptyCatalog_keepsLibraryAdministrationReachableForAdmins() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 1,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 }),
        uiState = emptyCatalogUiState(isAdmin = true),
      )
    }

    composeRule.onNodeWithText("Libraries").assertIsDisplayed()
    composeRule.onAllNodesWithText("No Libraries are available.").assertCountEquals(0)
  }

  @Test
  fun deletingFinalLibrary_keepsAdministrationPageReachableAndClampsPager() {
    val uiState = mutableStateOf(libraryCatalogUiState())
    lateinit var pagerState: PagerState

    composeRule.setContent {
      pagerState =
        rememberPagerState(
          initialPage = 1,
          pageCount = { uiState.value.librariesUiState.size + 1 },
        )
      HomeScreenContent(
        libraryCount = uiState.value.librariesUiState.size + 1,
        pagerState = pagerState,
        uiState = uiState.value,
      )
    }

    composeRule.onNodeWithText("Libraries").assertIsDisplayed()
    composeRule.runOnIdle {
      uiState.value = emptyCatalogUiState(isAdmin = true)
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Libraries").assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(0, pagerState.currentPage) }
  }

  @Test
  fun emptyCatalog_doesNotExposeLibraryAdministrationToNonAdmins() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 1,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 }),
        uiState = emptyCatalogUiState(isAdmin = false),
      )
    }

    composeRule.onAllNodesWithText("Libraries").assertCountEquals(0)
  }

  private fun emptyCatalogUiState(isAdmin: Boolean): HomeUiState =
    HomeUiState(
      state = GenericState.Success,
      prefs = Prefs(userPrefs = UserPrefs(isAdmin = isAdmin)),
    )

  private fun libraryCatalogUiState(): HomeUiState =
    emptyCatalogUiState(isAdmin = true).copy(
      activeLibraryId = "books",
      librariesUiState = listOf(LibraryUiState(id = "books", name = "Books")),
    )
}
