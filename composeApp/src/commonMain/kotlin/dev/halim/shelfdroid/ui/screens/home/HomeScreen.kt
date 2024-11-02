package dev.halim.shelfdroid.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.ui.components.LibraryItemCover
import dev.halim.shelfdroid.ui.components.LibraryItemNoCover
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(paddingValues: PaddingValues) {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val libraryCount = uiState.librariesResponse.libraries.size

    if (libraryCount == 0) {
        Text(
            text = "No libraries available",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val bottomPadding = paddingValues.calculateBottomPadding()
    val pagerState = rememberPagerState(pageCount = { libraryCount })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.onEvent(HomeEvent.ChangeLibrary(page))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        HorizontalPager(
            modifier = Modifier.padding(bottom = bottomPadding),
            state = pagerState
        ) { page ->
            Column(modifier = Modifier.fillMaxSize()) {
                LibraryContent(
                    uiState = uiState,
                    modifier = Modifier.weight(1f)
                )
                LibraryHeader(
                    page = page,
                    uiState = uiState,
                    onRefresh = { viewModel.onEvent(HomeEvent.RefreshLibrary) }
                )
            }
        }
    }
}

@Composable
fun LibraryHeader(
    page: Int,
    uiState: HomeUiState,
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
            text = uiState.librariesResponse.libraries[page].name,
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
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = uiState.libraryItemsResponse.results.size - 1
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
            items = uiState.libraryItemsResponse.results,
            key = { it.id }
        ) { libraryItem ->
            var imageLoadFailed by remember { mutableStateOf(false) }

            if (imageLoadFailed) {
                LibraryItemNoCover()
            } else {
                LibraryItemCover(
                    itemId = libraryItem.id,
                    onError = { imageLoadFailed = true },
                    modifier = Modifier
                )
            }
        }
    }
}