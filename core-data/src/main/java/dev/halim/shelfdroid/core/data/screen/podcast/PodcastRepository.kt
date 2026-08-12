package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.ProgressRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.catalog.LibraryItemRepository
import dev.halim.shelfdroid.core.data.catalog.PodcastEpisodeRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.data.podcastsourcefeed.PodcastSourceFeedRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedDetails
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedMapper
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
  private val libraryItemRepo: LibraryItemRepository,
  private val progressRepo: ProgressRepository,
  private val downloadRepo: DownloadRepo,
  private val prefsRepository: PrefsRepository,
  private val api: ApiService,
  private val podcastEpisodeRepo: PodcastEpisodeRepository,
  private val podcastFeedRepo: PodcastSourceFeedRepository,
  private val mapper: PodcastMapper,
) {
  private val repositoryScope = CoroutineScope(Dispatchers.IO)

  fun observePodcast(id: String): Flow<PodcastUiState> {
    val entity = libraryItemRepo.flowById(id)
    val episodes = podcastEpisodeRepo.flowByLibraryItemId(id)
    val progresses = progressRepo.flowByLibraryItemId(id)
    val downloadSignal = combine(downloadRepo.downloads, downloadRepo.durableDownloads) { _, _ -> }
    val prefs = prefsRepository.prefsFlow()

    return combine(
      entity,
      episodes,
      progresses,
      downloadSignal,
      prefs,
    ) { entity, episodes, progresses, _, prefs ->
      entity?.let {
        val mappedEpisodes = mapper.mapEpisodes(it.title, episodes, progresses)
        val generatedRssFeed =
          GeneratedRssFeedMapper.map(
            itemId = id,
            feed = it.rssFeed?.let { rssFeed -> Json.decodeFromString(rssFeed) },
            webBaseUrl = currentWebBaseUrl(),
            canManage = prefs.userPrefs.isAdmin,
            hasAudioContent = episodes.isNotEmpty(),
            hasEpisodesWithoutPubDate = episodes.any { episode -> episode.pubDate == null },
          )

        PodcastUiState(
          state = GenericState.Success,
          author = it.author,
          title = it.title,
          cover = it.cover,
          description = it.description,
          canAddEpisode = prefs.userPrefs.isAdmin,
          canEditEpisode = prefs.userPrefs.isAdmin || prefs.userPrefs.update,
          canDeleteEpisode = prefs.userPrefs.isAdmin || prefs.userPrefs.delete,
          generatedRssFeed = generatedRssFeed,
          episodes = mappedEpisodes,
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

  suspend fun fetchPodcastSourceFeed(itemId: String): PodcastApiState {
    val feedUrl =
      libraryItemRepo.podcastById(itemId)?.metadata?.feedUrl?.takeIf { it.isNotBlank() }
        ?: refreshFeedUrl(itemId)
        ?: return failureState("Podcast source feed not found")

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

  suspend fun openGeneratedRssFeed(
    itemId: String,
    details: GeneratedRssFeedDetails,
  ): Result<Unit> {
    return libraryItemRepo.openGeneratedRssFeedForItem(itemId = itemId, details = details).map {}
  }

  suspend fun closeGeneratedRssFeed(itemId: String, feedId: String): Result<Unit> {
    return libraryItemRepo.closeGeneratedRssFeedForItem(itemId = itemId, feedId = feedId)
  }

  private suspend fun refreshFeedUrl(itemId: String): String? {
    val item = api.item(itemId).getOrNull() ?: return null
    libraryItemRepo.updateItem(item)
    return (item.media as? Podcast)?.metadata?.feedUrl?.takeIf { it.isNotBlank() }
  }

  private fun failureState(message: String) = PodcastApiState.AddFailure(message)

  private fun currentWebBaseUrl(): String = libraryItemRepo.currentWebBaseUrlForUi()
}
