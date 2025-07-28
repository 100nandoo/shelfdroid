package dev.halim.shelfdroid.core.data.screen.podcast

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.media.DownloadMapper
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject

class PodcastMapper
@Inject
constructor(private val helper: Helper, private val downloadMapper: DownloadMapper) {

  @SuppressLint("UnsafeOptInUsageError")
  suspend fun mapEpisodes(
    episodes: List<PodcastEpisode>,
    progresses: List<ProgressEntity>,
    downloads: List<Download>,
  ): List<Episode> =
    episodes
      .sortedByDescending { it.publishedAt }
      .map { podcastEpisode ->
        val progress = progresses.find { it.episodeId == podcastEpisode.id }
        val downloadId = helper.generateDownloadId(podcastEpisode.libraryItemId, podcastEpisode.id)
        val download = downloads.find { it.request.id == downloadId }
        Episode(
          episodeId = podcastEpisode.id,
          title = podcastEpisode.title,
          publishedAt = podcastEpisode.publishedAt?.let { helper.toReadableDate(it) } ?: "",
          progress = progress?.progress?.toFloat() ?: 0f,
          isFinished = progress?.isFinished == 1L,
          downloadId = downloadId,
          url = helper.generateContentUrl(podcastEpisode.audioTrack.contentUrl),
          downloadState = downloadMapper.toDownloadState(download?.state),
        )
      }
}
