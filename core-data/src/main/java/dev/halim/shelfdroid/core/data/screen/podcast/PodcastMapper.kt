package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class PodcastMapper
@Inject
constructor(private val helper: Helper, private val downloadRepository: DownloadRepo) {

  suspend fun mapEpisodes(
    episodes: List<PodcastEpisode>,
    progresses: List<ProgressEntity>,
  ): List<Episode> =
    episodes
      .sortedByDescending { it.publishedAt }
      .map { podcastEpisode ->
        val progress = progresses.find { it.episodeId == podcastEpisode.id }

        val downloadUiState =
          downloadRepository.item(
            itemId = podcastEpisode.libraryItemId,
            episodeId = podcastEpisode.id,
            url = podcastEpisode.audioTrack.contentUrl,
            title = podcastEpisode.title,
          )
        Episode(
          episodeId = podcastEpisode.id,
          title = podcastEpisode.title,
          publishedAt = podcastEpisode.publishedAt?.let { helper.toReadableDate(it) } ?: "",
          progress = progress?.progress?.toFloat() ?: 0f,
          isFinished = progress?.isFinished == 1L,
          download = downloadUiState,
        )
      }

  fun mapAddEpisodes(
    episodesFromDb: List<PodcastEpisode>,
    response: PodcastFeed,
  ): List<AddEpisode> {

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
        description = feedEpisode.description,
        pubDate = feedEpisode.pubDate,
        publishedAt = feedEpisode.publishedAt,
        url = url,
        state =
          if (dbEpisode != null) AddEpisodeDownloadState.Downloaded
          else AddEpisodeDownloadState.NotDownloaded,
      )
    }
  }
}
