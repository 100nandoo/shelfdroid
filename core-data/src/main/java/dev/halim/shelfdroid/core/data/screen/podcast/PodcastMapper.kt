package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.response.libraryitem.PodcastEpisode
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
}
