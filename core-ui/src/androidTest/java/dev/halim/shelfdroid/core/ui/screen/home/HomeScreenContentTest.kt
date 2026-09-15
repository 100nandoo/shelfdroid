package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.HomeCatalogState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryDataSyncState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.prefs.BookSort
import dev.halim.shelfdroid.core.prefs.DisplayPrefs
import dev.halim.shelfdroid.core.prefs.PodcastSort
import dev.halim.shelfdroid.core.prefs.Prefs
import dev.halim.shelfdroid.core.prefs.SortOrder
import dev.halim.shelfdroid.core.prefs.UserPrefs
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun emptyCatalog_keepsLibraryAdminReachableForAdmins() {
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
  fun emptyCatalog_doesNotExposeLibraryAdminToNonAdmins() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 1,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 }),
        uiState = emptyCatalogUiState(isAdmin = false),
      )
    }

    composeRule.onAllNodesWithText("Libraries").assertCountEquals(0)
  }

  @Test
  fun emptyOfflineCatalog_explainsThatNoCachedDataIsAvailable() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 1,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 }),
        uiState =
          emptyCatalogUiState(isAdmin = false)
            .copy(libraryDataSyncState = LibraryDataSyncState.Offline),
      )
    }

    composeRule
      .onNodeWithText("No cached library data is available while offline.")
      .assertIsDisplayed()
  }

  @Test
  fun unresolvedOfflineCatalog_showsProgressWithoutEmptyMessage() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 1,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 }),
        uiState =
          emptyCatalogUiState(isAdmin = false)
            .copy(
              catalog = HomeCatalogState.Loading,
              libraryDataSyncState = LibraryDataSyncState.Offline,
            ),
      )
    }

    composeRule
      .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
      .assertIsDisplayed()
    composeRule
      .onAllNodesWithText("No cached library data is available while offline.")
      .assertCountEquals(0)
  }

  @Test
  fun offlineCatalog_keepsCachedLibrariesVisible() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 2,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }),
        uiState =
          libraryCatalogUiState()
            .copy(libraryDataSyncState = LibraryDataSyncState.Offline),
      )
    }

    composeRule.onNodeWithText("Books").assertIsDisplayed()
  }

  @Test
  fun failureWithoutMessage_showsSafeGenericMessage() {
    composeRule.setContent {
      HomeScreenContent(
        libraryCount = 1,
        pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 }),
        uiState =
          emptyCatalogUiState(isAdmin = false)
            .copy(libraryDataSyncState = LibraryDataSyncState.Failed()),
      )
    }

    composeRule.onNodeWithText("Unable to load library data. Please try again.").assertIsDisplayed()
  }

  @Test
  fun addedAtSort_displaysBookDateInBothLayoutsAndDirections() {
    val addedAt = localTimestamp(2026, 1, 7)
    val state =
      mutableStateOf(
        homeUiState(
          prefs = Prefs(displayPrefs = DisplayPrefs(bookSort = BookSort.AddedAt)),
          librariesUiState =
            listOf(
              LibraryUiState(
                id = "books",
                name = "Books",
                books =
                  listOf(
                    BookUiState(
                      id = "book",
                      title = "Book",
                      author = "Author",
                      addedAt = addedAt,
                    )
                  ),
              )
            ),
        )
      )

    composeRule.setContent {
      AnimatedPreviewWrapper {
        HomeScreenContent(
          libraryCount = 2,
          pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }),
          uiState = state.value,
        )
      }
    }

    for (listView in listOf(true, false)) {
      for (sortOrder in listOf(SortOrder.Asc, SortOrder.Desc)) {
        composeRule.runOnIdle {
          state.value =
            state.value.copy(
              prefs =
                state.value.prefs.copy(
                  displayPrefs =
                    state.value.prefs.displayPrefs.copy(
                      listView = listView,
                      bookSort = BookSort.AddedAt,
                      sortOrder = sortOrder,
                    )
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Added 7 Jan 2026").assertIsDisplayed()
      }
    }
  }

  @Test
  fun addedAtSort_displaysPodcastDateInBothLayoutsAndDirections() {
    val addedAt = localTimestamp(2025, 12, 27)
    val state =
      mutableStateOf(
        homeUiState(
          prefs = Prefs(displayPrefs = DisplayPrefs(podcastSort = PodcastSort.AddedAt)),
          librariesUiState =
            listOf(
              LibraryUiState(
                id = "podcasts",
                name = "Podcasts",
                isBookLibrary = false,
                podcasts =
                  listOf(
                    PodcastUiState(
                      id = "podcast",
                      title = "Podcast",
                      author = "Author",
                      addedAt = addedAt,
                    )
                  ),
              )
            ),
        )
      )

    composeRule.setContent {
      AnimatedPreviewWrapper {
        HomeScreenContent(
          libraryCount = 2,
          pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }),
          uiState = state.value,
        )
      }
    }

    for (listView in listOf(true, false)) {
      for (sortOrder in listOf(SortOrder.Asc, SortOrder.Desc)) {
        composeRule.runOnIdle {
          state.value =
            state.value.copy(
              prefs =
                state.value.prefs.copy(
                  displayPrefs =
                    state.value.prefs.displayPrefs.copy(
                      listView = listView,
                      podcastSort = PodcastSort.AddedAt,
                      podcastSortOrder = sortOrder,
                    )
                )
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Added 27 Dec 2025").assertIsDisplayed()
      }
    }
  }

  @Test
  fun switchingBookAwayFromAddedAt_restoresAuthorSecondaryText() {
    val state =
      mutableStateOf(
        homeUiState(
          prefs = Prefs(displayPrefs = DisplayPrefs(bookSort = BookSort.AddedAt)),
          librariesUiState =
            listOf(
              LibraryUiState(
                id = "books",
                name = "Books",
                books =
                  listOf(
                    BookUiState(
                      id = "book",
                      title = "Book",
                      author = "Author",
                      addedAt = localTimestamp(2026, 1, 7),
                    )
                  ),
              )
            ),
        )
      )

    composeRule.setContent {
      AnimatedPreviewWrapper {
        HomeScreenContent(
          libraryCount = 2,
          pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }),
          uiState = state.value,
        )
      }
    }

    composeRule.onNodeWithText("Added 7 Jan 2026").assertIsDisplayed()
    composeRule.runOnIdle {
      state.value =
        state.value.copy(
          prefs = state.value.prefs.copy(displayPrefs = DisplayPrefs(bookSort = BookSort.Title))
        )
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Author").assertIsDisplayed()
    composeRule.onAllNodesWithText("Added 7 Jan 2026").assertCountEquals(0)
  }

  @Test
  fun switchingPodcastAwayFromAddedAt_restoresAuthorSecondaryText() {
    val state =
      mutableStateOf(
        homeUiState(
          prefs = Prefs(displayPrefs = DisplayPrefs(podcastSort = PodcastSort.AddedAt)),
          librariesUiState =
            listOf(
              LibraryUiState(
                id = "podcasts",
                name = "Podcasts",
                isBookLibrary = false,
                podcasts =
                  listOf(
                    PodcastUiState(
                      id = "podcast",
                      title = "Podcast",
                      author = "Author",
                      addedAt = localTimestamp(2025, 12, 27),
                    )
                  ),
              )
            ),
        )
      )

    composeRule.setContent {
      AnimatedPreviewWrapper {
        HomeScreenContent(
          libraryCount = 2,
          pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }),
          uiState = state.value,
        )
      }
    }

    composeRule.onNodeWithText("Added 27 Dec 2025").assertIsDisplayed()
    composeRule.runOnIdle {
      state.value =
        state.value.copy(
          prefs =
            state.value.prefs.copy(displayPrefs = DisplayPrefs(podcastSort = PodcastSort.Title))
        )
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Author").assertIsDisplayed()
    composeRule.onAllNodesWithText("Added 27 Dec 2025").assertCountEquals(0)
  }

  private fun emptyCatalogUiState(isAdmin: Boolean): HomeUiState =
    HomeUiState(
      catalog = HomeCatalogState.Ready(emptyList()),
      libraryDataSyncState = LibraryDataSyncState.Synced,
      prefs = Prefs(userPrefs = UserPrefs(isAdmin = isAdmin)),
    )

  private fun libraryCatalogUiState(): HomeUiState =
    emptyCatalogUiState(isAdmin = true)
      .copy(
        activeLibraryId = "books",
        catalog =
          HomeCatalogState.Ready(listOf(LibraryUiState(id = "books", name = "Books"))),
      )

  private fun homeUiState(
    prefs: Prefs,
    librariesUiState: List<LibraryUiState>,
  ): HomeUiState =
    HomeUiState(
      catalog = HomeCatalogState.Ready(librariesUiState),
      libraryDataSyncState = LibraryDataSyncState.Synced,
      prefs = prefs,
      activeLibraryId = librariesUiState.firstOrNull()?.id,
    )

  private fun localTimestamp(year: Int, month: Int, day: Int): Long =
    ZonedDateTime.of(year, month, day, 12, 0, 0, 0, ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()
}
