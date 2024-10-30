package dev.halim.shelfdroid.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(paddingValues: PaddingValues) {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val libraryCount = uiState.librariesResponse.libraries.size

    if (libraryCount > 0) {
        val bottomPadding = paddingValues.calculateBottomPadding()
        val pagerState = rememberPagerState(pageCount = { libraryCount })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom
        ) {
            LibraryContent(pagerState.currentPage, uiState, Modifier.weight(1f))
            HorizontalPager(
                modifier = Modifier.padding(bottom = bottomPadding),
                state = pagerState
            ) { page ->
                PagerContent(page, uiState)
            }

        }
    } else {
        Text(
            text = "No libraries available",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun PagerContent(page: Int, uiState: HomeUiState) {
    Text(
        text = uiState.librariesResponse.libraries[page].name,
        modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.White),
        textAlign = TextAlign.Center
    )
}

@Composable
fun LibraryContent(page: Int, uiState: HomeUiState, modifier: Modifier = Modifier) {
    val color = if (page % 2 == 0) Color.Red else Color.Green
    Text(
        text = uiState.librariesResponse.libraries[page].name,
        modifier = modifier.fillMaxWidth().fillMaxWidth().background(color),
        textAlign = TextAlign.Center
    )
}