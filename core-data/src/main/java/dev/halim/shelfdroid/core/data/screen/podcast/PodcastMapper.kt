package dev.halim.shelfdroid.core.data.screen.podcast

import android.annotation.SuppressLint
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject

class PodcastMapper
@Inject
constructor(private val helper: Helper, private val downloadRepository: DownloadRepo) {

  @SuppressLint("UnsafeOptInUsageError")
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
            podcastEpisode.libraryItemId,
            podcastEpisode.id,
            podcastEpisode.audioTrack.contentUrl,
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
