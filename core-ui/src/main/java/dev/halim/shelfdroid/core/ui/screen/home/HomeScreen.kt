@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.home

import Item
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.home.HomeState
import dev.halim.shelfdroid.core.data.home.HomeUiState
import dev.halim.shelfdroid.core.data.home.ShelfdroidMediaItem
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun HomeScreen(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  viewModel: HomeViewModel = hiltViewModel(),
  onBookClicked: (String) -> Unit,
  onPodcastClicked: (String) -> Unit,
  onSettingsClicked: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val libraryCount = uiState.librariesUiState.size

  val pagerState = rememberPagerState(pageCount = { libraryCount })
  var lastPage by remember { mutableIntStateOf(pagerState.currentPage) }
  var isGridScrolling by remember { mutableStateOf(false) }

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
      .collect { page ->
        if (page != lastPage) {
          viewModel.onEvent(HomeEvent.ChangeLibrary(page))
          lastPage = page
        }
      }
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

  CompositionLocalProvider(
    LocalSharedTransitionScope provides sharedTransitionScope,
    LocalAnimatedContentScope provides animatedContentScope,
  ) {
    Scaffold(
      floatingActionButton = {
        AnimatedVisibility(
          visible = !pagerState.isScrollInProgress && !isGridScrolling,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
          FloatingActionButton(onClick = { onSettingsClicked() }) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
          }
        }
      }
    ) { paddingValues ->
      HomeScreenContent(
        Modifier.padding(paddingValues),
        libraryCount,
        pagerState,
        uiState,
        { homeEvent -> viewModel.onEvent(homeEvent) },
        loadingIndicatorAlpha,
        onGridScrollStateChanged = { isScrolling -> isGridScrolling = isScrolling },
      )
    }
  }
}

@Composable
fun HomeScreenContent(
  modifier: Modifier = Modifier,
  libraryCount: Int = 1,
  pagerState: PagerState = rememberPagerState { 1 },
  uiState: HomeUiState = HomeUiState(),
  onEvent: (HomeEvent) -> Unit = {},
  loadingIndicatorAlpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
  onGridScrollStateChanged: (Boolean) -> Unit = {},
) {
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
        onScrollStateChanged = onGridScrollStateChanged,
      )
      LibraryHeader(
        name = uiState.librariesUiState[page].name,
        onRefresh = { onEvent(HomeEvent.RefreshLibrary(page)) },
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
fun LibraryHeader(name: String, onRefresh: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Text(text = name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    IconButton(onClick = onRefresh) {
      Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = "refresh",
        tint = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
fun LibraryContent(
  modifier: Modifier = Modifier,
  list: List<ShelfdroidMediaItem>?,
  onEvent: (HomeEvent) -> Unit,
  onScrollStateChanged: (Boolean) -> Unit = {},
) {
  if (list?.isNotEmpty() == true) {
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = 0)

    LaunchedEffect(gridState) {
      snapshotFlow { gridState.isScrollInProgress }
        .collect { isScrolling -> onScrollStateChanged(isScrolling) }
    }

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Adaptive(minSize = 160.dp),
      modifier = modifier.fillMaxSize(),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      items(items = list, key = { it.id }) { libraryItem ->
        Item(uiState = libraryItem, onEvent = onEvent)
      }
    }
  }
}
