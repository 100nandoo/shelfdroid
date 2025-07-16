package dev.halim.shelfdroid.core.ui.screen.home

import Item
import androidx.activity.compose.ReportDrawnWhen
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.data.screen.home.ShelfdroidMediaItem
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun HomeScreen(
  viewModel: HomeViewModel = hiltViewModel(),
  onBookClicked: (String) -> Unit,
  onPodcastClicked: (String) -> Unit,
  onSettingsClicked: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val libraryCount = uiState.librariesUiState.size

  val pagerState = rememberPagerState(pageCount = { libraryCount })

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
  ReportDrawnWhen { uiState.libraryItemsUiState.isNotEmpty() }
  if (libraryCount == 0 && uiState.homeState is HomeState.Success) {
    GenericMessageScreen("No libraries available.")
  } else {
    val homeState = uiState.homeState
    if (homeState is HomeState.Failure) {
      GenericMessageScreen(homeState.errorMessage ?: "")
    }
  }

  HorizontalPager(state = pagerState) { page ->
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
      LibraryContent(
        modifier = Modifier.weight(1f),
        list = uiState.libraryItemsUiState[page],
        onEvent = onEvent,
        name = uiState.librariesUiState[page].name,
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
fun LibraryHeader(name: String, onRefresh: () -> Unit, onSettingsClicked: () -> Unit) {
  Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
    MyIconButton(
      icon = Icons.Filled.Refresh,
      contentDescription = "Refresh",
      onClick = onRefresh,
      size = 28,
    )
    Text(
      text = name,
      Modifier.padding(horizontal = 8.dp).weight(1f),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Start,
    )
    MyIconButton(
      icon = Icons.Default.Settings,
      contentDescription = "Settings",
      onClick = onSettingsClicked,
      size = 32,
    )
  }
}

@Composable
fun LibraryContent(
  modifier: Modifier = Modifier,
  list: List<ShelfdroidMediaItem>?,
  onEvent: (HomeEvent) -> Unit,
  name: String,
  onRefresh: () -> Unit,
  onSettingsClicked: () -> Unit,
) {
  if (list?.isNotEmpty() == true) {
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 0)

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Fixed(2),
      modifier = modifier.fillMaxSize(),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        LibraryHeader(name = name, onRefresh = onRefresh, onSettingsClicked = onSettingsClicked)
      }
      items(items = list, key = { it.id }) { libraryItem ->
        Item(uiState = libraryItem, onEvent = onEvent)
      }
    }
  }
}

private val previewHomeUiState =
  HomeUiState(
    homeState = HomeState.Success,
    librariesUiState =
      listOf(
        LibraryUiState(id = "1", name = "My Books"),
        LibraryUiState(id = "2", name = "My Podcasts"),
      ),
    libraryItemsUiState =
      mapOf(
        0 to
          listOf(
            BookUiState(
              id = Defaults.BOOK_ID,
              author = Defaults.BOOK_AUTHOR,
              title = Defaults.BOOK_TITLE,
              cover = Defaults.BOOK_COVER,
              progress = 0.3f,
            ),
            BookUiState(
              id = "book2",
              author = "George R. R. Martin",
              title = "A Game of Thrones",
              cover = Defaults.BOOK_COVER,
              progress = 0.7f,
            ),
          ),
        1 to
          listOf(
            PodcastUiState(
              id = "podcast1",
              author = Defaults.AUTHOR_NAME,
              title = Defaults.TITLE,
              cover = Defaults.IMAGE_URL,
              episodeCount = Defaults.EPISODES.size,
            )
          ),
      ),
  )

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { HomeScreenContent(uiState = previewHomeUiState) }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { HomeScreenContent(uiState = previewHomeUiState) }
}
