@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import dev.halim.shelfdroid.core.ui.screen.home.item.HomeItem
import dev.halim.shelfdroid.core.ui.screen.home.item.HomeItemBottomSheet
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  viewModel: HomeViewModel = hiltViewModel(),
  onBookClicked: (String) -> Unit,
  onPodcastClicked: (String) -> Unit,
  onSettingsClicked: () -> Unit,
  onSearchClicked: (String) -> Unit,
  onSessionClicked: () -> Unit,
  onOpenSessionClicked: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val libraryCount = uiState.librariesUiState.size + 1

  val pagerState = rememberPagerState(pageCount = { libraryCount }, initialPage = 1)
  LaunchedEffect(pagerState.currentPage) {
    viewModel.onEvent(HomeEvent.ChangeLibrary(pagerState.currentPage))
  }

  HomeScreenContent(
    Modifier,
    libraryCount,
    pagerState,
    uiState,
    { homeEvent -> viewModel.onEvent(homeEvent) },
    onBookClicked,
    onPodcastClicked,
    onSettingsClicked,
    onSearchClicked,
    onSessionClicked,
    onOpenSessionClicked,
  )
}

@Composable
fun HomeScreenContent(
  modifier: Modifier = Modifier,
  libraryCount: Int = 1,
  pagerState: PagerState = rememberPagerState { 1 },
  uiState: HomeUiState = HomeUiState(),
  onEvent: (HomeEvent) -> Unit = {},
  onBookClicked: (String) -> Unit = {},
  onPodcastClicked: (String) -> Unit = {},
  onSettingsClicked: () -> Unit = {},
  onSearchClicked: (String) -> Unit = {},
  onSessionClicked: () -> Unit = {},
  onOpenSessionClicked: () -> Unit = {},
) {
  if (libraryCount == 0 && uiState.homeState is HomeState.Success) {
    GenericMessageScreen(stringResource(R.string.no_libraries_available))
  } else {
    val homeState = uiState.homeState
    if (homeState is HomeState.Failure) {
      GenericMessageScreen(homeState.errorMessage ?: "")
    }
  }

  HorizontalPager(state = pagerState) { page ->
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
      VisibilityDown(uiState.homeState is HomeState.Loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
      if (pagerState.pageCount - 1 == page) {
        MiscScreen(onOpenSessionClicked, onSessionClicked, onSettingsClicked)
      } else {
        val library = uiState.librariesUiState[page]
        LibraryContent(
          modifier = Modifier.weight(1f),
          prefs = uiState.prefs,
          books = library.books,
          podcasts = library.podcasts,
          onEvent = onEvent,
          id = uiState.librariesUiState[page].id,
          name = uiState.librariesUiState[page].name,
          isBookLibrary = uiState.librariesUiState[page].isBookLibrary,
          onFilterChange = {
            onEvent(HomeEvent.HomeDisplayPrefsEvent(DisplayPrefsEvent.Filter(it)))
          },
          onBookSortChange = {
            onEvent(HomeEvent.HomeDisplayPrefsEvent(DisplayPrefsEvent.BookSort(it)))
          },
          onPodcastSortChange = {
            onEvent(HomeEvent.HomeDisplayPrefsEvent(DisplayPrefsEvent.PodcastSort(it)))
          },
          onSortOrderChange = {
            onEvent(HomeEvent.HomeDisplayPrefsEvent(DisplayPrefsEvent.SortOrder(it)))
          },
          onPodcastSortOrderChange = {
            onEvent(HomeEvent.HomeDisplayPrefsEvent(DisplayPrefsEvent.PodcastSortOrder(it)))
          },
          onRefresh = { onEvent(HomeEvent.RefreshLibrary(page)) },
          onSearchClicked = onSearchClicked,
          onBookClicked = onBookClicked,
          onPodcastClicked = onPodcastClicked,
        )
      }
    }
  }
}

@Composable
fun LibraryHeader(
  id: String,
  name: String,
  isBookLibrary: Boolean,
  prefs: Prefs = Prefs(),
  onFilterChange: (String) -> Unit,
  onBookSortChange: (String) -> Unit,
  onPodcastSortChange: (String) -> Unit,
  onSortOrderChange: (String) -> Unit,
  onPodcastSortOrderChange: (String) -> Unit,
  onRefresh: () -> Unit,
  onSearchClicked: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()

  val displayPrefsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val showAddPodcast = isBookLibrary.not() && prefs.userPrefs.isAdmin
  DisplayPrefsSheet(
    displayPrefsSheetState,
    prefs.displayPrefs,
    isBookLibrary,
    onFilterChange,
    onBookSortChange,
    onPodcastSortChange,
    onSortOrderChange,
    onPodcastSortOrderChange,
  )

  Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = name,
      Modifier.padding(horizontal = 8.dp).weight(1f),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Start,
    )
    if (showAddPodcast) {
      MyIconButton(
        icon = Icons.Filled.Add,
        contentDescription = stringResource(R.string.add_podcast),
        onClick = { onSearchClicked(id) },
        size = 48,
      )
    }
    MyIconButton(
      icon = Icons.AutoMirrored.Filled.Sort,
      contentDescription = stringResource(R.string.sort_and_filter),
      onClick = { scope.launch { displayPrefsSheetState.show() } },
      size = 48,
    )

    MyIconButton(
      icon = Icons.Filled.Refresh,
      contentDescription = stringResource(R.string.refresh),
      onClick = onRefresh,
      size = 48,
    )
  }
}

@Composable
fun LibraryContent(
  modifier: Modifier = Modifier,
  prefs: Prefs,
  books: List<BookUiState>,
  podcasts: List<PodcastUiState>,
  onEvent: (HomeEvent) -> Unit,
  id: String,
  name: String,
  isBookLibrary: Boolean,
  onBookClicked: (String) -> Unit,
  onPodcastClicked: (String) -> Unit,
  onFilterChange: (String) -> Unit,
  onBookSortChange: (String) -> Unit,
  onPodcastSortChange: (String) -> Unit,
  onSortOrderChange: (String) -> Unit,
  onPodcastSortOrderChange: (String) -> Unit,
  onRefresh: () -> Unit,
  onSearchClicked: (String) -> Unit,
) {
  val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 0)
  val displayPrefs = prefs.displayPrefs
  val listView = displayPrefs.listView
  val columnCount = remember(listView) { if (listView) 1 else 3 }
  val isDownloaded = displayPrefs.filter.isDownloaded()
  val processedBooks = bookFilterAndSort(books, displayPrefs)
  val processedPodcasts = podcastFilterAndSort(podcasts, displayPrefs)

  val scope = rememberCoroutineScope()

  val sheetState = rememberModalBottomSheetState()
  var selectedBook by remember { mutableStateOf(BookUiState()) }
  var selectedPodcast by remember { mutableStateOf(PodcastUiState()) }
  var isBook by remember { mutableStateOf(false) }

  val canDelete = prefs.userPrefs.delete

  fun onLongClick(book: BookUiState?, podcast: PodcastUiState?) {
    if (canDelete.not()) return
    if (book != null) {
      selectedBook = book
    } else if (podcast != null) {
      selectedPodcast = podcast
    }
    isBook = book != null
    scope.launch { sheetState.show() }
  }

  if (sheetState.isVisible) {
    HomeItemBottomSheet(
      sheetState = sheetState,
      isBook = isBook,
      selectedBook = selectedBook,
      selectedPodcast = selectedPodcast,
      initialHardDelete = prefs.crudPrefs.hardDelete,
      onDelete = {
        val itemId = if (isBook) selectedBook.id else selectedPodcast.id
        onEvent(HomeEvent.Delete(id, itemId, isBook, it))
      },
    )
  }

  LazyVerticalGrid(
    state = gridState,
    columns = GridCells.Fixed(columnCount),
    modifier = modifier.fillMaxSize(),
    reverseLayout = true,
    verticalArrangement = Arrangement.Bottom,
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      LibraryHeader(
        id = id,
        name = name,
        isBookLibrary = isBookLibrary,
        prefs = prefs,
        onFilterChange = onFilterChange,
        onBookSortChange = onBookSortChange,
        onPodcastSortChange = onPodcastSortChange,
        onSortOrderChange = onSortOrderChange,
        onPodcastSortOrderChange = onPodcastSortOrderChange,
        onRefresh = onRefresh,
        onSearchClicked = onSearchClicked,
      )
    }
    items(items = processedBooks, key = { it.id }) { book ->
      HomeItem(
        listView = listView,
        id = book.id,
        title = book.title,
        author = book.author,
        cover = book.cover,
        onClick = { onBookClicked(book.id) },
        onLongClick = { onLongClick(book, null) },
      )
      if (listView) {
        HorizontalDivider()
      }
    }
    items(items = processedPodcasts, key = { it.id }) { podcast ->
      val count =
        remember(isDownloaded) {
          if (isDownloaded) podcast.unfinishedAndDownloadCount else podcast.unfinishedCount
        }
      HomeItem(
        listView = listView,
        id = podcast.id,
        title = podcast.title,
        author = podcast.author,
        cover = podcast.cover,
        unfinishedEpisodeCount = count,
        onClick = { onPodcastClicked(podcast.id) },
        onLongClick = { onLongClick(null, podcast) },
      )
      if (listView) {
        HorizontalDivider()
      }
    }
  }
}

private fun bookFilterAndSort(
  books: List<BookUiState>,
  displayPrefs: DisplayPrefs,
): List<BookUiState> {
  val filtered = books.filter { !displayPrefs.filter.isDownloaded() || it.isDownloaded }

  val comparator =
    when (displayPrefs.bookSort) {
      BookSort.AddedAt -> compareBy<BookUiState> { it.addedAt }
      BookSort.Duration -> compareBy { it.duration }
      BookSort.Title -> compareBy { it.title }
      BookSort.Progress -> compareBy { it.progressLastUpdate }
    }

  return if (displayPrefs.sortOrder == SortOrder.Desc) {
    filtered.sortedWith(comparator.reversed())
  } else {
    filtered.sortedWith(comparator)
  }
}

private fun podcastFilterAndSort(
  podcasts: List<PodcastUiState>,
  displayPrefs: DisplayPrefs,
): List<PodcastUiState> {
  val filtered = podcasts.filter { !displayPrefs.filter.isDownloaded() || it.downloadedCount > 0 }

  val comparator =
    when (displayPrefs.podcastSort) {
      PodcastSort.AddedAt -> compareBy<PodcastUiState> { it.addedAt }
      PodcastSort.Title -> compareBy { it.title }
    }

  return if (displayPrefs.podcastSortOrder == SortOrder.Desc) {
    filtered.sortedWith(comparator.reversed())
  } else {
    filtered.sortedWith(comparator)
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    HomeScreenContent(
      pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 }),
      uiState = Defaults.HOME_UI_STATE,
      onSessionClicked = {},
    )
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    HomeScreenContent(
      pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 }),
      uiState = Defaults.HOME_UI_STATE,
      onSessionClicked = {},
    )
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentListPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    HomeScreenContent(
      pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 }),
      uiState = Defaults.HOME_UI_STATE_LIST,
      onSessionClicked = {},
    )
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentListDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    HomeScreenContent(
      pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 }),
      uiState = Defaults.HOME_UI_STATE_LIST,
      onSessionClicked = {},
    )
  }
}
