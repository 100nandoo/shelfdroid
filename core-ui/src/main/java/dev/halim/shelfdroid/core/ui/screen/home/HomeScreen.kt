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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.DisplayOptions
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
        displayOptions = uiState.displayOptions,
        books = library.books,
        podcasts = library.podcasts,
        onEvent = onEvent,
        name = uiState.librariesUiState[page].name,
        onDownloadFilterClicked = { onEvent(HomeEvent.DownloadFilter) },
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
  displayOptions: DisplayOptions = DisplayOptions(),
  onDownloadFilterClicked: () -> Unit,
  onRefresh: () -> Unit,
  onSettingsClicked: () -> Unit,
) {
  Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = name,
      Modifier.padding(horizontal = 8.dp).weight(1f),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Start,
    )
    val isDownloadedFilterOn = displayOptions.filter.isDownloaded()
    val icon =
      if (isDownloadedFilterOn) Icons.Filled.FileDownloadDone else Icons.Filled.FileDownload
    val contentDescription =
      if (isDownloadedFilterOn) stringResource(R.string.downloaded_filter_on)
      else stringResource(R.string.downloaded_filter_off)
    MyIconButton(
      icon = icon,
      contentDescription = contentDescription,
      onClick = onDownloadFilterClicked,
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
  displayOptions: DisplayOptions,
  books: List<BookUiState>,
  podcasts: List<PodcastUiState>,
  onEvent: (HomeEvent) -> Unit,
  name: String,
  onDownloadFilterClicked: () -> Unit,
  onRefresh: () -> Unit,
  onSettingsClicked: () -> Unit,
) {
  val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 0)
  val listView = displayOptions.listView
  val columnCount = remember(listView) { if (listView) 1 else 3 }
  val isDownloaded = displayOptions.filter.isDownloaded()
  val books = books.filter { if (isDownloaded) it.isDownloaded else true }
  val podcasts = podcasts.filter { if (isDownloaded) it.downloadedCount > 0 else true }

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
        displayOptions = displayOptions,
        onDownloadFilterClicked = onDownloadFilterClicked,
        onRefresh = onRefresh,
        onSettingsClicked = onSettingsClicked,
      )
    }
    items(items = books, key = { it.id }) { book ->
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
    items(items = podcasts, key = { it.id }) { podcast ->
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
