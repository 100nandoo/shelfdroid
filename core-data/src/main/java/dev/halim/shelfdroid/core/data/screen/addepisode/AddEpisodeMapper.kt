package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.response.PodcastFeed
import javax.inject.Inject

class AddEpisodeMapper @Inject constructor() {
  fun mapEpisodes(response: PodcastFeed): List<Episode> {
    // TODO is download should be retrieve from database
    return response.podcast.episodes.map {
      Episode(
        title = it.title,
        description = it.description,
        pubDate = it.pubDate,
        publishedAt = it.publishedAt,
        url = it.enclosure.url,
        isDownloaded = false,
      )
    }
  }
}
