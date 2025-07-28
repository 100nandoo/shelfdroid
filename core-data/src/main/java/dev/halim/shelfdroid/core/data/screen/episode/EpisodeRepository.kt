package dev.halim.shelfdroid.core.data.screen.episode

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.media.DownloadMapper
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import java.util.Locale
import javax.inject.Inject
import kotlinx.serialization.json.Json

class EpisodeRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val helper: Helper,
  private val downloadMapper: DownloadMapper,
) {
  @SuppressLint("UnsafeOptInUsageError")
  suspend fun item(itemId: String, episodeId: String): EpisodeUiState {
    val result = libraryItemRepo.byId(itemId)
    val progressEntity = progressRepo.episodeById(episodeId)

    return if (result != null && result.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(result.media)

      val episode =
        media.episodes.find { it.id == episodeId }
          ?: return EpisodeUiState(state = GenericState.Failure("Failed to find episode"))

      val description = episode.description ?: ""

      val progress = progressEntity?.progress?.toFloat() ?: 0f
      val formattedProgress = String.format(Locale.getDefault(), "%.0f", progress * 100)
      val publishedAt = episode.publishedAt?.let { helper.toReadableDate(it) } ?: ""

      val downloadUiState =
        downloadRepo.item(
          itemId = itemId,
          episodeId = episodeId,
          url = episode.audioTrack.contentUrl,
        )

      EpisodeUiState(
        state = GenericState.Success,
        title = episode.title,
        podcast = result.title,
        publishedAt = publishedAt,
        cover = result.cover,
        description = description,
        progress = formattedProgress,
        download = downloadUiState,
      )
    } else {
      EpisodeUiState(state = GenericState.Failure("Failed to fetch Podcast"))
    }
  }

  @SuppressLint("UnsafeOptInUsageError")
  fun updateDownloads(uiState: EpisodeUiState, download: Download): EpisodeUiState {
    val downloadUiState =
      uiState.download.copy(state = downloadMapper.toDownloadState(download.state))

    return uiState.copy(download = downloadUiState)
  }
}
