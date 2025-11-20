package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.response.SearchPodcast
import javax.inject.Inject

class SearchPodcastMapper @Inject constructor() {
  fun map(response: List<SearchPodcast>): List<SearchPodcastUi> {
    return response.map { podcast ->
      SearchPodcastUi(
        id = podcast.id,
        author = podcast.artistName,
        title = podcast.title,
        cover = podcast.cover,
        genre = podcast.genres.joinToString(),
        episodeCount = podcast.trackCount,
        feedUrl = podcast.feedUrl,
        pageUrl = podcast.pageUrl,
        explicit = podcast.explicit,
      )
    }
  }
}
