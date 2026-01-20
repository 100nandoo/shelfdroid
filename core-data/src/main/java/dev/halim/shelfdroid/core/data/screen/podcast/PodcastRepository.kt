package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.PodcastFeedRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
  private val prefsRepository: PrefsRepository,
  private val api: ApiService,
  private val podcastFeedRepo: PodcastFeedRepo,
  private val mapper: PodcastMapper,
) {
  private val repositoryScope = CoroutineScope(Dispatchers.IO)

  var podcast: Podcast? = null

  fun item(id: String): Flow<PodcastUiState> {
    val entity = libraryItemRepo.flowById(id)
    val progresses = progressRepo.flowByLibraryItemId(id)
    val prefs = prefsRepository.prefsFlow()

    return combine(entity, progresses, downloadRepo.downloads, prefs) {
      entity,
      progresses,
      downloads,
      prefs ->
      entity?.let {
        podcast = Json.decodeFromString<Podcast>(it.media)
        val episodes = mapper.mapEpisodes(podcast?.episodes ?: emptyList(), progresses)

        PodcastUiState(
          state = GenericState.Success,
          author = it.author,
          title = it.title,
          cover = it.cover,
          description = it.description,
          canAddEpisode = prefs.userPrefs.isAdmin,
          canDeleteEpisode = prefs.userPrefs.isAdmin || prefs.userPrefs.delete,
          episodes = episodes,
          prefs = prefs,
        )
      } ?: PodcastUiState(state = GenericState.Failure("Failed to fetch podcast"))
    }
  }

  suspend fun toggleIsFinished(itemId: String, episode: Episode): Boolean {
    val request = ProgressRequest(episode.isFinished.not())
    val result = api.patchPodcastProgress(itemId, episode.episodeId, request)

    if (result.isSuccess) {
      repositoryScope.launch {
        val entity = progressRepo.episodeById(episode.episodeId)
        if (entity != null) {
          progressRepo.toggleIsFinishedByEpisodeId(episode.episodeId)
        } else {
          progressRepo.markEpisodeFinished(itemId, episode.episodeId)
        }
      }
    }
    return result.isSuccess
  }

  suspend fun markIsFinished(itemId: String, episodeId: String): Boolean {
    val request = ProgressRequest(true)
    val result = api.patchPodcastProgress(itemId, episodeId, request)

    if (result.isSuccess) {
      repositoryScope.launch { progressRepo.markEpisodeFinished(itemId, episodeId) }
    }
    return result.isSuccess
  }

  suspend fun fetchEpisode(): PodcastApiState {
    if (podcast == null) return failureState("Podcast not found")

    val feedUrl = podcast?.metadata?.feedUrl ?: return failureState("Podcast feed URL not found")

    val result = podcastFeedRepo.fetch(feedUrl)
    return if (result is GenericState.Success) {
      PodcastApiState.AddSuccess
    } else failureState("Failed to fetch podcast feed")
  }

  suspend fun deleteEpisode(
    itemId: String,
    hardDelete: Boolean,
    episodeIds: Set<String>,
  ): Set<String> = coroutineScope {
    val hard = if (hardDelete) 1 else 0
    val failureIds =
      episodeIds
        .map { episodeId -> async { episodeId to api.deleteEpisode(itemId, episodeId, hard) } }
        .awaitAll()
        .filterNot { (_, result) -> result.isSuccess }
        .map { (episodeId, _) -> episodeId }
        .toSet()
    val toDeleteIds = episodeIds - failureIds
    libraryItemRepo.deleteEpisodes(itemId, toDeleteIds)
    failureIds
  }

  private fun failureState(message: String) = PodcastApiState.AddFailure(message)
}
