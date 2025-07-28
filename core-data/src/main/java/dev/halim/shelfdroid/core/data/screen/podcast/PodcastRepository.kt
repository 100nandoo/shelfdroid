package dev.halim.shelfdroid.core.data.screen.podcast

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import dev.halim.core.network.ApiService
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.media.DownloadMapper
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class PodcastRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val apiService: ApiService,
  private val mapper: PodcastMapper,
  private val downloadRepo: DownloadRepo,
  private val downloadMapper: DownloadMapper,
) {
  private val repositoryScope = CoroutineScope(Dispatchers.IO)

  suspend fun item(id: String): PodcastUiState {
    val entity = libraryItemRepo.byId(id)
    val progresses = progressRepo.entities()
    val downloads = downloadRepo.downloads.value
    return entity
      ?.takeIf { it.isBook == 0L }
      ?.let {
        val media = Json.decodeFromString<Podcast>(it.media)
        val episodes = mapper.mapEpisodes(media.episodes, progresses, downloads)

        PodcastUiState(
          state = GenericState.Success,
          author = it.author,
          title = it.title,
          cover = it.cover,
          description = it.description,
          episodes = episodes,
        )
      } ?: PodcastUiState(state = GenericState.Failure("Failed to fetch podcast"))
  }

  @SuppressLint("UnsafeOptInUsageError")
  fun updateDownloads(uiState: PodcastUiState, downloads: List<Download>): PodcastUiState {
    val downloadMap = downloads.associateBy { it.request.id }

    val updatedEpisodes =
      uiState.episodes.map { episode ->
        val downloadState = downloadMap[episode.downloadId]?.state
        if (downloadState != null) {
          episode.copy(downloadState = downloadMapper.toDownloadState(downloadState))
        } else {
          episode
        }
      }
    return uiState.copy(episodes = updatedEpisodes)
  }

  suspend fun toggleIsFinished(itemId: String, episode: Episode): Boolean {
    val request = ProgressRequest(episode.isFinished.not())
    val result = apiService.patchPodcastProgress(itemId, episode.episodeId, request)

    if (result.isSuccess) {
      repositoryScope.launch { progressRepo.updateMediaById(episode.episodeId) }
    }
    return result.isSuccess
  }
}
