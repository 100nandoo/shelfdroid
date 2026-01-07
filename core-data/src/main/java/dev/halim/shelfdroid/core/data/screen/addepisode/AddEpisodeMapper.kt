package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import javax.inject.Inject

class AddEpisodeMapper @Inject constructor() {
  fun mapEpisodes(episodesFromDb: List<PodcastEpisode>, response: PodcastFeed): List<Episode> {
    val dbEpisodeUrls = episodesFromDb.mapNotNull { it.enclosure?.url }

    return response.podcast.episodes.map {
      val isDownloaded = dbEpisodeUrls.contains(it.enclosure.url)
      val state =
        if (isDownloaded) AddEpisodeDownloadState.Downloaded
        else AddEpisodeDownloadState.NotDownloaded
      Episode(
        title = it.title,
        description = it.description,
        pubDate = it.pubDate,
        publishedAt = it.publishedAt,
        url = it.enclosure.url,
        state = state,
      )
    }
  }
}
