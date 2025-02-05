package dev.halim.shelfdroid.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.components.HomeItem
import dev.halim.shelfdroid.ui.generic.GenericMessageScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(paddingValues: PaddingValues, onBookClicked: (String) -> Unit, onPodcastClicked: (String) -> Unit) {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val libraryCount = uiState.librariesUiState.size

    val bottomPadding = paddingValues.calculateBottomPadding()
    val pagerState = rememberPagerState(pageCount = { libraryCount })
    var lastPage by remember { mutableStateOf(pagerState.currentPage) }

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
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

    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.homeState) {
        isLoading = when (uiState.homeState) {
            is HomeState.Loading -> true
            else -> false
        }
    }

    HomeScreenContent(
        libraryCount,
        bottomPadding,
        pagerState,
        uiState,
        playerState,
        { homeEvent -> viewModel.onEvent(homeEvent) },
        isLoading
    )
}

@Composable
fun HomeScreenContent(
    libraryCount: Int = 1,
    bottomPadding: Dp = 16.dp,
    pagerState: PagerState = rememberPagerState { 1 },
    uiState: HomeUiState = HomeUiState(),
    playerState: MediaPlayerState,
    onEvent: (HomeEvent) -> Unit = {},
    isLoading: Boolean = false
) {
    if (libraryCount == 0 && uiState.homeState is HomeState.Success) {
        GenericMessageScreen("No libraries available.")
    } else if (uiState.homeState is HomeState.Failure) {
        GenericMessageScreen(uiState.homeState.errorMessage ?: "")
    }

    HorizontalPager(
        modifier = Modifier.padding(bottom = bottomPadding),
        state = pagerState,
    ) { page ->
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            LibraryContent(
                modifier = Modifier.weight(1f),
                list = uiState.libraryItemsUiState[page],
                playerState = playerState,
                onEvent = onEvent,
                headerName = uiState.librariesUiState[page].name,
                onRefresh = { onEvent(HomeEvent.RefreshLibrary(page)) }
            )
            AnimatedVisibility(isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
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
    list: List<ShelfdroidMediaItem>?,
    playerState: MediaPlayerState,
    onEvent: (HomeEvent) -> Unit,
    headerName: String,
    onRefresh: () -> Unit,
) {
    if (list?.isNotEmpty() == true) {
        val gridState = rememberLazyGridState(
            initialFirstVisibleItemIndex = 0
        )
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            item(span = {
                GridItemSpan(maxLineSpan)
            }) {
                LibraryHeader(
                    name = headerName,
                    onRefresh = onRefresh
                )
            }
            items(
                items = list,
                key = { it.id }
            ) { libraryItem ->
                HomeItem(
                    uiState = libraryItem,
                    modifier = Modifier,
                    playerState = playerState,
                    onEvent,
                )
            }
        }
    }
}