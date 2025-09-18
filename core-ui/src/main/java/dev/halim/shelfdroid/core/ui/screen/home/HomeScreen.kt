@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import dev.halim.shelfdroid.core.ui.screen.home.item.HomeItem
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  viewModel: HomeViewModel = hiltViewModel(),
  onBookClicked: (String) -> Unit,
  onPodcastClicked: (String) -> Unit,
  onSettingsClicked: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val libraryCount = uiState.librariesUiState.size

  val pagerState = rememberPagerState(pageCount = { libraryCount })
  LaunchedEffect(pagerState.currentPage) {
    viewModel.onEvent(HomeEvent.ChangeLibrary(pagerState.currentPage))
  }

  LaunchedEffect(Unit) {
    viewModel.navState.collect { navUiState ->
      if (navUiState.isNavigate) {
        if (navUiState.isBook) {
          onBookClicked(navUiState.id)
        } else {
          onPodcastClicked(navUiState.id)
        }
        viewModel.resetNavigationState()
      }
    }
  }

  val loadingIndicatorAlpha = remember { Animatable(0f) }

  LaunchedEffect(uiState.homeState) {
    when (uiState.homeState) {
      is HomeState.Loading -> loadingIndicatorAlpha.animateTo(1f)
      else -> loadingIndicatorAlpha.animateTo(0f)
    }
  }

  HomeScreenContent(
    Modifier,
    libraryCount,
    pagerState,
    uiState,
    { homeEvent -> viewModel.onEvent(homeEvent) },
    loadingIndicatorAlpha,
    onSettingsClicked,
  )
}

@Composable
fun HomeScreenContent(
  modifier: Modifier = Modifier,
  libraryCount: Int = 1,
  pagerState: PagerState = rememberPagerState { 1 },
  uiState: HomeUiState = HomeUiState(),
  onEvent: (HomeEvent) -> Unit = {},
  loadingIndicatorAlpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
  onSettingsClicked: () -> Unit = {},
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
    val library = uiState.librariesUiState[page]

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
      LibraryContent(
        modifier = Modifier.weight(1f),
        displayPrefs = uiState.displayPrefs,
        books = library.books,
        podcasts = library.podcasts,
        onEvent = onEvent,
        name = uiState.librariesUiState[page].name,
        isBookLibrary = uiState.librariesUiState[page].isBookLibrary,
        onFilterChange = { onEvent(HomeEvent.Filter(it)) },
        onBookSortChange = { onEvent(HomeEvent.BookSort(it)) },
        onPodcastSortChange = { onEvent(HomeEvent.PodcastSort(it)) },
        onSortOrderChange = { onEvent(HomeEvent.SortOrder(it)) },
        onRefresh = { onEvent(HomeEvent.RefreshLibrary(page)) },
        onSettingsClicked = onSettingsClicked,
      )
      if (loadingIndicatorAlpha.value > 0f) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = loadingIndicatorAlpha.value }
        )
      }
    }
  }
}

@Composable
fun LibraryHeader(
  name: String,
  isBookLibrary: Boolean,
  displayPrefs: DisplayPrefs = DisplayPrefs(),
  onFilterChange: (String) -> Unit,
  onBookSortChange: (String) -> Unit,
  onPodcastSortChange: (String) -> Unit,
  onSortOrderChange: (String) -> Unit,
  onRefresh: () -> Unit,
  onSettingsClicked: () -> Unit,
) {
  val scope = rememberCoroutineScope()

  val displayPrefsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  DisplayPrefsSheet(
    displayPrefsSheetState,
    displayPrefs,
    isBookLibrary,
    onFilterChange,
    onBookSortChange,
    onPodcastSortChange,
    onSortOrderChange,
  )

  Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = name,
      Modifier.padding(horizontal = 8.dp).weight(1f),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Start,
    )
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
    MyIconButton(
      icon = Icons.Default.Settings,
      contentDescription = stringResource(R.string.settings),
      onClick = onSettingsClicked,
      size = 48,
    )
  }
}

@Composable
fun LibraryContent(
  modifier: Modifier = Modifier,
  displayPrefs: DisplayPrefs,
  books: List<BookUiState>,
  podcasts: List<PodcastUiState>,
  onEvent: (HomeEvent) -> Unit,
  name: String,
  isBookLibrary: Boolean,
  onFilterChange: (String) -> Unit,
  onBookSortChange: (String) -> Unit,
  onPodcastSortChange: (String) -> Unit,
  onSortOrderChange: (String) -> Unit,
  onRefresh: () -> Unit,
  onSettingsClicked: () -> Unit,
) {
  val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 0)
  val listView = displayPrefs.listView
  val columnCount = remember(listView) { if (listView) 1 else 3 }
  val isDownloaded = displayPrefs.filter.isDownloaded()
  val processedBooks = bookFilterAndSort(books, displayPrefs)
  val processedPodcasts = podcastFilterAndSort(podcasts, displayPrefs)

  LazyVerticalGrid(
    state = gridState,
    columns = GridCells.Fixed(columnCount),
    modifier = modifier.fillMaxSize(),
    reverseLayout = true,
    verticalArrangement = Arrangement.Bottom,
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      LibraryHeader(
        name = name,
        isBookLibrary = isBookLibrary,
        displayPrefs = displayPrefs,
        onFilterChange = onFilterChange,
        onBookSortChange = onBookSortChange,
        onPodcastSortChange = onPodcastSortChange,
        onSortOrderChange = onSortOrderChange,
        onRefresh = onRefresh,
        onSettingsClicked = onSettingsClicked,
      )
    }
    items(items = processedBooks, key = { it.id }) { book ->
      HomeItem(
        listView = listView,
        id = book.id,
        title = book.title,
        author = book.author,
        cover = book.cover,
        onClick = { onEvent(HomeEvent.Navigate(book.id, true)) },
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
        onClick = { onEvent(HomeEvent.Navigate(podcast.id, false)) },
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
      BookSort.Progress -> compareBy { it.addedAt }
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

  return if (displayPrefs.sortOrder == SortOrder.Desc) {
    filtered.sortedWith(comparator.reversed())
  } else {
    filtered.sortedWith(comparator)
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    HomeScreenContent(uiState = Defaults.HOME_UI_STATE)
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    HomeScreenContent(uiState = Defaults.HOME_UI_STATE)
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentListPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    HomeScreenContent(uiState = Defaults.HOME_UI_STATE_LIST)
  }
}

@ShelfDroidPreview
@Composable
fun HomeScreenContentListDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    HomeScreenContent(uiState = Defaults.HOME_UI_STATE_LIST)
  }
}
