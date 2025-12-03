package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.response.SearchPodcast
import dev.halim.shelfdroid.core.data.response.PodcastInfo
import javax.inject.Inject

class SearchPodcastMapper @Inject constructor() {
  fun map(
    response: List<SearchPodcast>,
    podcastInfoList: List<PodcastInfo>,
  ): List<SearchPodcastUi> {
    return response.map { podcast ->
      var id = ""
      val isAdded =
        podcastInfoList.any { podcastInfo ->
          val found =
            podcastInfo.itunesId == podcast.id ||
              podcastInfo.feedUrl == podcast.feedUrl ||
              (podcastInfo.title == podcast.title && podcastInfo.artist == podcast.artistName)
          id = podcastInfo.id
          found
        }
      SearchPodcastUi(
        id = id,
        itunesId = podcast.id,
        author = podcast.artistName,
        title = podcast.title,
        cover = podcast.cover,
        genre = podcast.genres.joinToString(),
        episodeCount = podcast.trackCount,
        feedUrl = podcast.feedUrl,
        pageUrl = podcast.pageUrl,
        explicit = podcast.explicit,
        isAdded = isAdded,
      )
    }
  }
}
