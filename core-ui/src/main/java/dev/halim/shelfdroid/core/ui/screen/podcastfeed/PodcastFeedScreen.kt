package dev.halim.shelfdroid.core.ui.screen.podcastfeed

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun PodcastFeedScreen(viewModel: PodcastFeedViewModel = hiltViewModel()) {
  PodcastFeedScreenContent(viewModel.rssFeed)
}

@Composable
fun PodcastFeedScreenContent(rssFeed: String) {
  Text("Podcast Feed Screen with rss = $rssFeed")
}
