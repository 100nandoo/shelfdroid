package dev.halim.shelfdroid.core.ui.screen.podcastfeed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.podcastfeed.PodcastFeedUiState

@Composable
fun PodcastFeedScreen(viewModel: PodcastFeedViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  PodcastFeedScreenContent(uiState = uiState)
}

@Composable
private fun PodcastFeedScreenContent(uiState: PodcastFeedUiState) {
  Column {
    AnimatedVisibility(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Text(text = uiState.title)
    Text(text = uiState.author)
    Text(text = uiState.feedUrl)
    Text(text = uiState.genres.joinToString())
    Text(text = uiState.type)
    Text(text = uiState.language)
    Text(text = uiState.description)
  }
}
