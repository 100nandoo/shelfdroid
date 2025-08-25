package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class PodcastRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val apiService: ApiService,
  private val mapper: PodcastMapper,
) {
  private val repositoryScope = CoroutineScope(Dispatchers.IO)

  fun item(id: String): Flow<PodcastUiState> {
    val entity = libraryItemRepo.flowById(id)
    val progresses = progressRepo.flowByLibraryItemId(id)

    return combine(entity, progresses, downloadRepo.downloads) { entity, progresses, downloads ->
      entity?.let {
        val media = Json.decodeFromString<Podcast>(it.media)
        val episodes = mapper.mapEpisodes(media.episodes, progresses)

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
