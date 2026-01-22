package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.PodcastFeedRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class AddEpisodeRepository
@Inject
constructor(
  private val prefsRepository: PrefsRepository,
  private val libraryItemRepo: LibraryItemRepo,
  private val podcastFeedRepo: PodcastFeedRepo,
  private val apiService: ApiService,
  private val mapper: AddEpisodeMapper,
  private val json: Json,
) {
  lateinit var podcast: Podcast
  lateinit var podcastFeed: PodcastFeed
  val crudPrefs = prefsRepository.crudPrefs

  fun item(id: String): AddEpisodeUiState {
    val entity = libraryItemRepo.byId(id) ?: return failureState("Failed to fetch podcast")

    podcast = decodePodcast(entity.media) ?: return failureState("Invalid podcast data")

    val feedUrl = podcast.metadata.feedUrl
    podcastFeed =
      podcastFeedRepo.cache[feedUrl] ?: return failureState("Failed to fetch podcast feed")

    val episodes: List<AddEpisode> = mapper.mapEpisodes(podcast.episodes, podcastFeed)

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

  private fun decodePodcast(raw: String): Podcast? =
    runCatching { json.decodeFromString<Podcast>(raw) }.getOrNull()

  private fun failureState(message: String) =
    AddEpisodeUiState(state = GenericState.Failure(message))
}
