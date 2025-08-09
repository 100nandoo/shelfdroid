package dev.halim.shelfdroid.core.data.screen.episode

import android.annotation.SuppressLint
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class EpisodeRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val helper: Helper,
) {
  @SuppressLint("UnsafeOptInUsageError")
  fun item(itemId: String, episodeId: String): Flow<EpisodeUiState> {
    val podcastFlow = libraryItemRepo.flowById(itemId)
    val progressFlow = progressRepo.flowEpisodeById(episodeId)
    val downloadId = helper.generateDownloadId(itemId, episodeId)
    val download =
      downloadRepo.downloads.map { downloads -> downloads.find { it.request.id == downloadId } }

    return combine(podcastFlow, progressFlow, download) { podcast, progress, download ->
      podcast
        ?.takeIf { it.isBook == 0L }
        ?.let {
          val media = Json.decodeFromString<Podcast>(podcast.media)

          val episode =
            media.episodes.find { it.id == episodeId }
              ?: return@combine EpisodeUiState(
                state = GenericState.Failure("Failed to find episode")
              )

          val description = episode.description ?: ""

          val progress = progress?.progress?.toFloat() ?: 0f
          val formattedProgress = String.format(Locale.getDefault(), "%.0f", progress * 100)
          val publishedAt = episode.publishedAt?.let { helper.toReadableDate(it) } ?: ""

          val downloadUiState =
            downloadRepo.item(
              itemId = itemId,
              episodeId = episodeId,
              url = episode.audioTrack.contentUrl,
              title = episode.title,
            )

          EpisodeUiState(
            state = GenericState.Success,
            title = episode.title,
            podcast = podcast.title,
            publishedAt = publishedAt,
            cover = podcast.cover,
            description = description,
            progress = formattedProgress,
            download = downloadUiState,
          )
        } ?: EpisodeUiState(state = GenericState.Failure("Failed to fetch Podcast"))
    }
  }
}
