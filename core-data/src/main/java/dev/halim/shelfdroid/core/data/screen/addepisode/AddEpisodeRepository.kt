package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.catalog.LibraryItemRepository
import dev.halim.shelfdroid.core.data.catalog.PodcastEpisodeRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.podcastsourcefeed.PodcastSourceFeedRepository
import javax.inject.Inject

class AddEpisodeRepository
@Inject
constructor(
  private val prefsRepository: PrefsRepository,
  private val libraryItemRepo: LibraryItemRepository,
  private val podcastEpisodeRepo: PodcastEpisodeRepository,
  private val podcastFeedRepo: PodcastSourceFeedRepository,
  private val apiService: ApiService,
  private val mapper: AddEpisodeMapper,
) {
  lateinit var podcastFeed: PodcastFeed
  val crudPrefs = prefsRepository.crudPrefs

  suspend fun loadEpisodeSelection(id: String): AddEpisodeUiState {
    val entity = libraryItemRepo.byId(id) ?: return failureState("Failed to fetch podcast")
    val podcast = loadPodcast(id) ?: return failureState("Invalid podcast data")

    val feedUrl =
      podcast.metadata.feedUrl?.takeIf { it.isNotBlank() }
        ?: return failureState("Podcast source feed not found")
    podcastFeed =
      podcastFeedRepo.cache[feedUrl] ?: return failureState("Failed to fetch podcast feed")

    val episodes = mapper.mapEpisodes(podcastEpisodeRepo.byLibraryItemId(id), podcastFeed)

    return AddEpisodeUiState(
      state = GenericState.Success,
      author = entity.author,
      title = entity.title,
      cover = entity.cover,
      episodes = episodes,
    )
  }

  suspend fun downloadEpisodes(id: String, addEpisodes: List<AddEpisode>): GenericState {
    val episodes =
      podcastFeed.podcast.episodes.filter { episode ->
        episode.enclosure.url in addEpisodes.map { it.url }
      }
    val result = apiService.downloadEpisodes(id, episodes)
    return if (result.isSuccess) {
      GenericState.Success
    } else {
      GenericState.Failure(result.exceptionOrNull()?.message ?: "Failed to download episodes")
    }
  }

  private suspend fun loadPodcast(id: String): Podcast? {
    val cached = libraryItemRepo.podcastById(id)
    if (cached?.metadata?.feedUrl.isNullOrBlank().not()) {
      return cached
    }

    val remote = apiService.item(id).getOrNull() ?: return cached
    libraryItemRepo.updateItem(remote)
    return remote.media as? Podcast ?: cached
  }

  private fun failureState(message: String) =
    AddEpisodeUiState(state = GenericState.Failure(message))
}
