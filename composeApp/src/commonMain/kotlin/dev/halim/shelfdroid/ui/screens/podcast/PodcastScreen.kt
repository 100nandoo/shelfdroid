package dev.halim.shelfdroid.ui.screens.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.ui.components.ItemBasicContent
import dev.halim.shelfdroid.ui.components.ItemPlayIcon
import dev.halim.shelfdroid.ui.components.ItemPlayIconSize
import dev.halim.shelfdroid.ui.components.ItemProgressIndicator
import dev.halim.shelfdroid.utility.toPercent
import dev.halim.shelfdroid.utility.toReadableDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PodcastScreen(paddingValues: PaddingValues, id: String,
                  onEpisodeClicked: (String) -> Unit) {
    val viewModel: PodcastViewModel = koinViewModel(parameters = { parametersOf(id) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(PodcastUiState())
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    if (uiState.state == PodcastState.Success) {
        PodcastDetailContent(
            paddingValues, playerState, { event -> viewModel.onEvent(event) }, uiState.cover, uiState.title,
            uiState.author, uiState.description,
            uiState.episodes, onEpisodeClicked
        )
    }
}

@Composable
fun PodcastDetailContent(
    paddingValues: PaddingValues = PaddingValues(),
    playerState: MediaPlayerState = MediaPlayerState(),
    onEvent: (PodcastEvent) -> Unit,
    imageUrl: String = "",
    title: String = "Chapter 26",
    authorName: String = "Adam",
    description: String = "This is very cool podcast",
    episodes: List<EpisodeUiState> = emptyList(),
    onEpisodeClicked: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(episodes) { episode ->
            EpisodeItem(episode, playerState, onEvent, onEpisodeClicked)
        }
        item {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Left
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(description)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ItemBasicContent(imageUrl, title, authorName)
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: EpisodeUiState,
    playerState: MediaPlayerState,
    onEvent: (PodcastEvent) -> Unit = {},
    onEpisodeClicked: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemPlayIcon(episode, { onEvent(PodcastEvent.Play(episode)) }, playerState, ItemPlayIconSize.Small)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEpisodeClicked(episode.id) }
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = episode.progress.toPercent(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = episode.publishedAt.toReadableDate(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            ItemProgressIndicator(Modifier.fillMaxWidth(), episode.progress)
        }

    }
}