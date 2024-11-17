package dev.halim.shelfdroid.preview

import ShelfDroidPreview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.ui.components.HomeLibraryItem
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.HomeState
import dev.halim.shelfdroid.ui.screens.home.HomeUiState
import dev.halim.shelfdroid.ui.screens.home.LibraryHeader
import dev.halim.shelfdroid.ui.screens.home.LibraryUiState


val homeLibraryItemUiState =
    BookUiState(
        "1", mapOf("2" to 3.0), "Jane Doe", "The Art of Peace", "",
        0.0, 1.0, 0.0, "", 0
    )
val homeLibraryItemUiState2 =
    BookUiState(
        "2",
        mapOf("3" to 4.0),
        "Jane Doe, Marrow Slakovakovatich",
        "The Art of Consolidating Long Meeting",
        "",
        0.8,
        0.5,
        0.0,
        "",
        0
    )
val homeLibraryItemUiStateList = listOf(homeLibraryItemUiState, homeLibraryItemUiState2)
val libraryUiStateList = listOf(LibraryUiState("123", "Main"), LibraryUiState("234", "Podcast"))
val homeUiState = HomeUiState(
    HomeState.Success, libraryUiStateList, mapOf(
        (0 to homeLibraryItemUiStateList),
        (1 to homeLibraryItemUiStateList)
    )
)

@ShelfDroidPreview
@Composable
fun LibraryContentPreview() {
    ShelfDroidPreview {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            LibraryHeader(
                name = homeUiState.librariesUiState[0].name,
                onRefresh = { }
            )
        }
    }
}

@ShelfDroidPreview
@Composable
fun LibraryItemNoCoverPreview() {
    val response = homeLibraryItemUiStateList
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = response.size - 1
    )
    ShelfDroidPreview {
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
                HomeLibraryItem(item, true, {}, {}, Modifier)
            }
        }
    }
}