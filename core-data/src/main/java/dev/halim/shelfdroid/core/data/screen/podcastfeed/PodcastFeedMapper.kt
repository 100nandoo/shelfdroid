package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject

class PodcastFeedMapper @Inject constructor() {
  fun map(response: PodcastFeed): PodcastFeedUiState {
    return PodcastFeedUiState(
      state = GenericState.Success,
      title = response.podcast.metadata.title,
      author = response.podcast.metadata.author,
      feedUrl = response.podcast.metadata.feedUrl,
      genres = response.podcast.metadata.categories,
      type = response.podcast.metadata.type,
      language = response.podcast.metadata.language,
      explicit = response.podcast.metadata.explicit == "true",
      description = response.podcast.metadata.description,
      // TODO get folder from LibraryEntity
      folder = "",
      // TODO combine folder + title
      path = "",
    )
  }
}
