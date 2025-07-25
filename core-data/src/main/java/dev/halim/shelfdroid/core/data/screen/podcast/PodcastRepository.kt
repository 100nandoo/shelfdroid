package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.ProgressEntity
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
  private val helper: Helper,
) {
  private val repositoryScope = CoroutineScope(Dispatchers.IO)

  suspend fun item(id: String): PodcastUiState {
    val entity = libraryItemRepo.byId(id)
    val progresses = progressRepo.entities()

    return entity
      ?.takeIf { it.isBook == 0L }
      ?.let {
        val media = Json.decodeFromString<Podcast>(it.media)
        val episodes = mapEpisodes(media.episodes, progresses)

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

  suspend fun toggleIsFinished(itemId: String, episode: Episode): Boolean {
    val request = ProgressRequest(episode.isFinished.not())
    val result = apiService.patchPodcastProgress(itemId, episode.id, request)

    if (result.isSuccess) {
      repositoryScope.launch { progressRepo.updateMediaById(episode.id) }
    }
    return result.isSuccess
  }

  private fun mapEpisodes(
    episodes: List<PodcastEpisode>,
    progresses: List<ProgressEntity>,
  ): List<Episode> =
    episodes
      .sortedByDescending { it.publishedAt }
      .map { podcastEpisode ->
        val progress = progresses.find { it.episodeId == podcastEpisode.id }
        Episode(
          id = podcastEpisode.id,
          title = podcastEpisode.title,
          publishedAt = podcastEpisode.publishedAt?.let { helper.toReadableDate(it) } ?: "",
          progress = progress?.progress?.toFloat() ?: 0f,
          isFinished = progress?.isFinished == 1L,
          url = helper.generateContentUrl(podcastEpisode.audioTrack.contentUrl),
        )
      }
}
