package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Arrangement
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
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.home.HomeState
import dev.halim.shelfdroid.core.data.home.HomeUiState
import dev.halim.shelfdroid.core.data.home.ShelfdroidMediaItem
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel(), onBookClicked: (String) ->
Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val libraryCount = uiState.librariesUiState.size

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
        viewModel.navState.collect { (isNavigate, id) ->
            if (isNavigate) {
                onBookClicked(id)
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
        pagerState,
        uiState,
        { homeEvent -> viewModel.onEvent(homeEvent) },
        loadingIndicatorAlpha
    )
}

@Composable
fun HomeScreenContent(
    libraryCount: Int = 1,
    pagerState: PagerState = rememberPagerState { 1 },
    uiState: HomeUiState = HomeUiState(),
    onEvent: (HomeEvent) -> Unit = {},
    loadingIndicatorAlpha: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
) {
    if (libraryCount == 0 && uiState.homeState is HomeState.Success) {
        GenericMessageScreen("No libraries available.")
    } else {
        val homeState = uiState.homeState
        if (homeState is HomeState.Failure) {
            GenericMessageScreen(homeState.errorMessage ?: "")
        }
    }

    HorizontalPager(
        state = pagerState,
    ) { page ->
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            LibraryContent(
                modifier = Modifier.weight(1f),
                list = uiState.libraryItemsUiState[page],
                onEvent = onEvent
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
    onEvent: (HomeEvent) -> Unit,
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
                .padding(8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(
                items = list,
                key = { it.id }
            ) { libraryItem ->
//                HomeBook(
//                    uiState = libraryItem,
//                    modifier = Modifier,
//                    onEvent,
//                )
            }
        }
    }

}