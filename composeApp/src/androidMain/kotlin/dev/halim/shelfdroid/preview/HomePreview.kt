package dev.halim.shelfdroid.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.theme.ShelfDroidTheme
import dev.halim.shelfdroid.ui.components.HomeLibraryItem
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.HomeUiState
import dev.halim.shelfdroid.ui.screens.home.LibraryHeader

val homeUiState = HomeUiState()
val homeLibraryItemUiState =
    BookUiState("1", "2", "Jane Doe", "The Art of Peace", "", 0.0, 1.0, 0.0)
val homeLibraryItemUiState2 =
    BookUiState("2", "3", "Jane Doe, Marrow Slakovakovatich", "The Art of Consolidating Long Meeting", "", 0.8, 0.5, 0.0)
val homeLibraryItemUiStateList = listOf(homeLibraryItemUiState, homeLibraryItemUiState2)

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LibraryContentPreview() {
    ShelfDroidTheme(false) {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                verticalArrangement = Arrangement.Bottom
            ) {
                LibraryHeader(
                    page = 0,
                    uiState = homeUiState,
                    onRefresh = { }
                )
            }

        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DarkLibraryContentScreenPreview() {
    ShelfDroidTheme(true) {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                verticalArrangement = Arrangement.Bottom
            ) {
                LibraryHeader(
                    page = 0,
                    uiState = homeUiState,
                    onRefresh = { }
                )
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LibraryItemNoCoverPreview() {
    val response = homeLibraryItemUiStateList
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = response.size - 1
    )
    ShelfDroidTheme(false) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(
                items = response,
                key = { it.id }
            ) { item ->
                HomeLibraryItem(item, true, {})
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DarkLibraryItemNoCoverPreview() {
    val response = homeLibraryItemUiStateList
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = response.size - 1
    )
    ShelfDroidTheme(true) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom
        ) {
            items(
                items = response,
                key = { it.id }
            ) { item ->
                HomeLibraryItem(item, true, {})
            }
        }
    }
}