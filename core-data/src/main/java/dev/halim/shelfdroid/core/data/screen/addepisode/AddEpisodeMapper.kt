package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class AddEpisodeMapper @Inject constructor(private val helper: Helper) {

  fun mapEpisodes(episodesFromDb: List<PodcastEpisode>, response: PodcastFeed): List<AddEpisode> {

    val dbEpisodesByUrl =
      episodesFromDb
        .mapNotNull { episode -> episode.enclosure?.url?.let { url -> url to episode } }
        .toMap()

    return response.podcast.episodes.map { feedEpisode ->
      val url = feedEpisode.enclosure.url
      val dbEpisode = dbEpisodesByUrl[url]

      AddEpisode(
        episodeId = dbEpisode?.id.orEmpty(),
        title = feedEpisode.title,
        description = feedEpisode.descriptionPlain.trim(),
        pubDate = helper.toReadableDate(feedEpisode.publishedAt),
        publishedAt = feedEpisode.publishedAt,
        url = url,
        state =
          if (dbEpisode != null) AddEpisodeDownloadState.Downloaded
          else AddEpisodeDownloadState.NotDownloaded,
      )
    }
  }
}
