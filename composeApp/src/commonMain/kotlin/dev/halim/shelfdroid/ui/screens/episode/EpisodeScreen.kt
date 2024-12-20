package dev.halim.shelfdroid.ui.screens.episode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.ui.generic.GenericState
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EpisodeScreen(paddingValues: PaddingValues, id: String, episodeId: String) {
    val viewModel: EpisodeViewModel = koinViewModel(parameters = { parametersOf(id, episodeId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(EpisodeScreenUiState())


    if (uiState.state == GenericState.Success) {
        EpisodeScreenContent(paddingValues, uiState)
    }

}

@Composable
fun EpisodeScreenContent(paddingValues: PaddingValues, uiState: EpisodeScreenUiState) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(uiState.episodeUiState.title, style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            AnnotatedString.Companion.fromHtml(
                uiState.episodeUiState.description,
                TextLinkStyles(
                    style = SpanStyle(color = MaterialTheme.colorScheme.tertiary),
                    pressedStyle = SpanStyle(MaterialTheme.colorScheme.secondary)
                )
            )
        )
    }
}