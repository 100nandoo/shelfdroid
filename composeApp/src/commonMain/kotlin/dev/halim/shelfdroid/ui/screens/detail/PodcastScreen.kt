package dev.halim.shelfdroid.ui.screens.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PodcastScreen(paddingValues: PaddingValues, id: String) {
    val viewModel: PodcastViewModel = koinViewModel(parameters = { parametersOf(id) })

    PodcastDetailContent(paddingValues, id)
}

@Composable
fun PodcastDetailContent(
    paddingValues: PaddingValues = PaddingValues(),
    id: String = "",
) {
    Text("Podcast $id", modifier = Modifier.padding(16.dp).padding(paddingValues))
}