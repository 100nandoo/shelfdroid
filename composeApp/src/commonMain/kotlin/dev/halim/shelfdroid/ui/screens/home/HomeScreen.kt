package dev.halim.shelfdroid.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.ui.components.HomeLibraryItem
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(paddingValues: PaddingValues, onBookClicked: (BookUiState) -> Unit) {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val libraryCount = uiState.librariesUiState.size

    val bottomPadding = paddingValues.calculateBottomPadding()
    val pagerState = rememberPagerState(pageCount = { libraryCount })
    var lastPage by remember { mutableStateOf(pagerState.currentPage) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != lastPage) {
                viewModel.onEvent(HomeEvent.ChangeLibrary(page))
                lastPage = page
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navState.collect { (isNavigate, bookUiState) ->
            if (isNavigate){
                onBookClicked(bookUiState)
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
        libraryCount,
        bottomPadding,
        pagerState,
        uiState,
        { homeEvent -> viewModel.onEvent(homeEvent) },
        loadingIndicatorAlpha
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
    libraryCount: Int = 1,
    bottomPadding: Dp = 16.dp,
    pagerState: PagerState = rememberPagerState { 1 },
    uiState: HomeUiState = HomeUiState(),
    onEvent: (HomeEvent) -> Unit = {},
    loadingIndicatorAlpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
) {
    if (libraryCount == 0 && uiState.homeState == HomeState.Success) {
        NoLibrary()
    }

    HorizontalPager(
        modifier = Modifier.padding(bottom = bottomPadding),
        state = pagerState,
    ) { page ->
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            LibraryContent(
                modifier = Modifier.weight(1f),
                list = uiState.libraryItemsUiState[page],
            )
            LibraryHeader(
                name = uiState.librariesUiState[page].name,
                onRefresh = { onEvent(HomeEvent.RefreshLibrary(page)) }
            )
            if (loadingIndicatorAlpha.value > 0f) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = loadingIndicatorAlpha.value
                        }
                )
            }
        }
    }
}

@Composable
fun NoLibrary() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No libraries available",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
fun LibraryHeader(
    name: String,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "refresh",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LibraryContent(
    modifier: Modifier = Modifier,
    list: List<HomeLibraryItemUiState>?
) {
    if (list?.isNotEmpty() == true) {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = list.size - 1
        )
        val mediaManager = koinInject<MediaManager>()

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(
                items = list,
                key = { it.id }
            ) { libraryItem ->
                var imageLoadFailed by remember { mutableStateOf(false) }

                HomeLibraryItem(
                    uiState = libraryItem,
                    showNoCover = imageLoadFailed,
                    onImageError = { imageLoadFailed = true },
                    onPlayPauseClick = {
                        if (libraryItem is BookUiState) {
                            mediaManager.playBookUiState(libraryItem)
                        }
                    },
                    modifier = Modifier
                )
            }
        }
    }

}